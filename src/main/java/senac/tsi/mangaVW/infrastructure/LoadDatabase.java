package senac.tsi.mangaVW.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.mangaVW.entities.*;
import senac.tsi.mangaVW.repositories.*;

import static org.springframework.aot.hint.TypeReference.listOf;

@Configuration
public class LoadDatabase {
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(MangaRepository mangaRepository,
                                   AuthorRepository authorRepository,
                                   GenreRepository genreRepository,
                                   ChapterRepository chapterRepository,
                                   MangaDetailsRepository mangaDetailsRepository,
                                   PageRepository pageRepository) {
        return args -> {

            //Authors
            Author auMiura = new Author("Kentaro Miura", "Famoso autor de obras de fantasia sombria.");
            auMiura = authorRepository.save(auMiura);

            Author auKubo = new Author("Kubo Tite", "Autor do grande mangá shounen bleach...");
            auKubo = authorRepository.save(auKubo);


            //Genres
            Genre genFantasy = new Genre("Fantasia");
            genFantasy = genreRepository.save(genFantasy);

            Genre genFight = new Genre("Luta");
            genFight = genreRepository.save(genFight);

            Genre genCyberpunk = new Genre("Cyberpunk");
            genCyberpunk = genreRepository.save(genCyberpunk);

            MangaDetails mangaDetailsBerserk = new MangaDetails("123121251-1215", 1990, true);


            MangaDetails mangaDetailsBleach = new MangaDetails("123121251-1215", 2005, false);


            //Mangás

            Manga manga_1 = new Manga("Berserk", "A história de Guts...", StatusPublication.FINALIZADO);
            manga_1.setAuthor(auMiura);
            manga_1.setDetails(mangaDetailsBerserk);
            manga_1.getGenres().add(genFantasy);
            manga_1.getGenres().add(genFight);
            manga_1 = mangaRepository.save(manga_1);



            Manga manga_2 = new Manga("Bleach", "Uma história curta...", StatusPublication.FINALIZADO);
            manga_2.setAuthor(auKubo);
            manga_2.setDetails(mangaDetailsBleach);
            manga_2.getGenres().add(genFantasy);
            manga_2 = mangaRepository.save(manga_2);

            log.info("Mangá preloaded com sucesso!");

            //Chapters

            Chapter cap1Berserk = new Chapter(1.0, "pt-br");
            cap1Berserk.setManga(manga_1);
            cap1Berserk = chapterRepository.save(cap1Berserk);


            Chapter cap2Berserk = new Chapter(2.0, "pt-br");
            cap2Berserk.setManga(manga_1);
            cap2Berserk = chapterRepository.save(cap2Berserk);

            Chapter cap1Bleach = new Chapter(1.0, "pt-br");
            cap1Bleach.setManga(manga_2);
            cap1Bleach = chapterRepository.save(cap1Bleach);

            // Pages

            Page pag1Berserk = new Page(1, "https://exemplo.com/berserk/cap1/pag1.jpg");
            pag1Berserk.setChapter(cap1Berserk); // Vínculo obrigatório com o Capítulo (Pai)
            pageRepository.save(pag1Berserk);

            Page pag2Berserk = new Page(2, "https://exemplo.com/berserk/cap1/pag2.jpg");
            pag2Berserk.setChapter(cap1Berserk);
            pageRepository.save(pag2Berserk);

            Page pag3Berserk = new Page(1, "https://exemplo.com/berserk/cap1/pag1.jpg");
            pag3Berserk.setChapter(cap2Berserk); // Vínculo obrigatório com o Capítulo (Pai)
            pageRepository.save(pag3Berserk);

            Page pag4Berserk = new Page(2, "https://exemplo.com/berserk/cap1/pag2.jpg");
            pag4Berserk.setChapter(cap2Berserk);
            pageRepository.save(pag4Berserk);

            // Criando uma Página para o Capítulo 1 de Bleach
            Page pag1Bleach = new Page(1, "https://exemplo.com/bleach/cap1/pag1.jpg");
            pag1Bleach.setChapter(cap1Bleach);
            pageRepository.save(pag1Bleach);


            log.info("Capítulos e Páginas preloaded com sucesso!");
        };
    }
}