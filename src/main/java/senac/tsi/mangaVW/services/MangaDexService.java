package senac.tsi.mangaVW.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import senac.tsi.mangaVW.entities.*;
import senac.tsi.mangaVW.repositories.*;

@Service
public class MangaDexService {

    // Injetando TODO o nosso banco de dados
    private final MangaRepository mangaRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final MangaDetailsRepository detailsRepository;
    private final ChapterRepository chapterRepository;
    private final PageRepository pageRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public MangaDexService(MangaRepository mangaRepository, AuthorRepository authorRepository,
                           GenreRepository genreRepository, MangaDetailsRepository detailsRepository,
                           ChapterRepository chapterRepository, PageRepository pageRepository) {
        this.mangaRepository = mangaRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.detailsRepository = detailsRepository;
        this.chapterRepository = chapterRepository;
        this.pageRepository = pageRepository;
        this.restTemplate = new RestTemplate();
    }

    public void syncMangasFromMangaDex() throws Exception {
        // 1. URL super turbinada: Pedindo os dados do Mangá + Autor + Capa (cover_art)
        String url = "https://api.mangadex.org/manga?limit=10&includes[]=author&includes[]=cover_art";

        String jsonString = restTemplate.getForObject(url, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.readTree(jsonString);

        if (response != null && response.has("data")) {
            JsonNode dataArray = response.get("data");

            for (JsonNode mangaNode : dataArray) {
                JsonNode attributes = mangaNode.get("attributes");

                // --- 1. TÍTULO E SINOPSE ---
                String title = attributes.has("title") && attributes.get("title").has("en")
                        ? attributes.get("title").get("en").asText()
                        : (attributes.has("title") && attributes.get("title").has("ja-ro") ? attributes.get("title").get("ja-ro").asText() : "Título Desconhecido");

                String sinopsis = "Sem sinopse.";
                if (attributes.has("description") && attributes.get("description").has("en")) {
                    sinopsis = attributes.get("description").get("en").asText();
                }
                if (sinopsis.length() > 250) sinopsis = sinopsis.substring(0, 247) + "...";

                // Só processa se o mangá ainda não existir
                if (mangaRepository.findByTitleContainingIgnoreCase(title, Pageable.unpaged()).isEmpty()) {

                    // --- 2. STATUS DO MANGÁ ---
                    StatusPublication status = StatusPublication.EM_ANDAMENTO;
                    if (attributes.has("status")) {
                        String dexStatus = attributes.get("status").asText();
                        if ("completed".equals(dexStatus)) status = StatusPublication.FINALIZADO;
                        else if ("cancelled".equals(dexStatus)) status = StatusPublication.CANCELADO;
                    }

                    // --- 3. DETALHES (MangaDetails) ---
                    int year = attributes.has("year") && !attributes.get("year").isNull() ? attributes.get("year").asInt() : 2024;
                    MangaDetails details = new MangaDetails();
                    details.setPublicationYear(year);
                    details.setLicensed(false);
                    details.setIsbn("000-0000000000"); // Mock genérico
                    details = detailsRepository.save(details);

                    // --- 4. AUTOR ---
                    String authorName = "Autor Desconhecido";
                    String coverFileName = null; // Para a imagem mais tarde
                    String mangaId = mangaNode.get("id").asText();

                    if (mangaNode.has("relationships")) {
                        for (JsonNode rel : mangaNode.get("relationships")) {
                            if ("author".equals(rel.get("type").asText()) && rel.has("attributes")) {
                                authorName = rel.get("attributes").get("name").asText();
                            }
                            if ("cover_art".equals(rel.get("type").asText()) && rel.has("attributes")) {
                                coverFileName = rel.get("attributes").get("fileName").asText();
                            }
                        }
                    }

                    var authorPage = authorRepository.findByNameContainingIgnoreCase(authorName, Pageable.unpaged());
                    Author autorReal = authorPage.isEmpty()
                            ? authorRepository.save(new Author(authorName, "Importado via MangaDex"))
                            : authorPage.getContent().get(0);

                    // --- 5. SALVANDO O MANGÁ ---
                    Manga manga = new Manga(title, sinopsis, status);
                    manga.setAuthor(autorReal);
                    manga.setDetails(details);
                    manga = mangaRepository.save(manga);

                    // --- 6. GÊNEROS (Tags do MangaDex) ---
                    if (attributes.has("tags")) {
                        for (JsonNode tagNode : attributes.get("tags")) {
                            String genreName = tagNode.get("attributes").get("name").get("en").asText();
                            var genrePage = genreRepository.findByNameContainingIgnoreCase(genreName, Pageable.unpaged());

                            Genre genre = genrePage.isEmpty()
                                    ? genreRepository.save(new Genre(genreName))
                                    : genrePage.getContent().get(0);

                            manga.getGenres().add(genre);
                        }
                        manga = mangaRepository.save(manga); // Atualiza o mangá com a lista de gêneros
                    }

                    // --- 7. IMAGEM (Capítulo e Página) ---
                    if (coverFileName != null) {
                        // A URL oficial de imagens do MangaDex
                        String imageUrl = "https://uploads.mangadex.org/covers/" + mangaId + "/" + coverFileName;

                        // Cria o Capítulo 1
                        Chapter chapter = new Chapter(1.0, "en");
                        chapter.setManga(manga);
                        chapter = chapterRepository.save(chapter);

                        // Cria a Página 1 com a Imagem
                        Page page = new Page(1, imageUrl);
                        page.setChapter(chapter);
                        pageRepository.save(page);
                    }

                    System.out.println("Mangá COMPLETO importado: " + title);
                }
            }
        }
    }
}