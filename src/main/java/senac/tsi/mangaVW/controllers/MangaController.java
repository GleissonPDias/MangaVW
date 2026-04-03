package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import senac.tsi.mangaVW.entities.Author;
import senac.tsi.mangaVW.entities.Genre;
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.exceptions.MangaNotFoundException;
import senac.tsi.mangaVW.repositories.AuthorRepository;
import senac.tsi.mangaVW.repositories.GenreRepository;
import senac.tsi.mangaVW.repositories.MangaRepository;
import senac.tsi.mangaVW.services.MangaDexService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="mangas", description = "Mangas route")
@RestController
@RequestMapping("/mangas")
public class MangaController {

    private final MangaRepository mangaRepository;
    private final PagedResourcesAssembler<Manga> pagedResourcesAssembler;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final MangaDexService mangaDexService;

    @Autowired
    public MangaController(MangaRepository mangaRepository,
                           PagedResourcesAssembler<Manga> pagedResourcesAssembler,
                           MangaDexService mangaDexService,
                           AuthorRepository authorRepository,
                           GenreRepository genreRepository) {
        this.mangaRepository = mangaRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.mangaDexService = mangaDexService;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
    }

    @Tag(name = "Search")
    @Operation(summary = "Search mangas by title", description = "Busca paginada de mangás contendo um título específico.")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Manga>>> searchMangasByTitle(
            @RequestParam String title,
            @ParameterObject Pageable pageable) {

        var mangas = mangaRepository.findByTitleContainingIgnoreCase(title, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(mangas));
    }

    @Operation(summary = "Sync mangas from MangaDex API", description = "Importa mangás da API pública do MangaDex.")
    @PostMapping("/sync")
    public ResponseEntity<String> syncFromMangaDex() {
        try {
            mangaDexService.syncMangasFromMangaDex();
            return ResponseEntity.ok("Sincronização com MangaDex concluída com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao sincronizar com MangaDex: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all mangas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Manga>>> getAllMangas(@ParameterObject Pageable pageable){
        var mangas = mangaRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(mangas));
    }

    @Operation(summary = "Get a single manga by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga encontrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido"),
            @ApiResponse(responseCode = "404", description = "Manga não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Manga>> getMangaById(@PathVariable long id){
        var manga = mangaRepository.findById(id)
                .orElseThrow(() -> new MangaNotFoundException(id));

        var entityModel = EntityModel.of(manga,
                linkTo(methodOn(MangaController.class).getMangaById(id)).withSelfRel(),
                linkTo(methodOn(MangaController.class).getAllMangas(Pageable.unpaged())).withRel("mangas"));

        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new manga")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manga criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
            @ApiResponse(responseCode = "404", description = "Autor ou Gênero informado não existe")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Dados do mangá. É obrigatório informar o ID do Autor. Gêneros e Detalhes são opcionais.",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "title": "Berserk",
                              "sinopsis": "Um jovem mercenário...",
                              "status": "EM_ANDAMENTO",
                              "author": { "id": 1 },
                              "genres": [
                                { "id": 1 },
                                { "id": 2 }
                              ],
                              "details": {
                                    "isbn": "123121251-1215",
                                    "licensed": false,
                                    "publicationYear": 2005
                              }
                            }
                            """)))
    @PostMapping
    public ResponseEntity<Manga> createManga(@Valid @RequestBody Manga newManga){

        // 1. Busca e valida o Autor
        if (newManga.getAuthor() == null || newManga.getAuthor().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Author author = authorRepository.findById(newManga.getAuthor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autor não encontrado"));
        newManga.setAuthor(author);

        // 2. Busca e valida a lista de Gêneros (se for enviada)
        if (newManga.getGenres() != null && !newManga.getGenres().isEmpty()) {
            List<Genre> fetchedGenres = new ArrayList<>();
            for (Genre g : newManga.getGenres()) {
                Genre foundGenre = genreRepository.findById(g.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gênero não encontrado: ID " + g.getId()));
                fetchedGenres.add(foundGenre);
            }
            newManga.setGenres(fetchedGenres);
        }

        Manga savedManga = mangaRepository.save(newManga);
        return ResponseEntity
                .created(URI.create("/mangas/"+ savedManga.getId()))
                .body(savedManga);
    }

    @Operation(summary = "Update an existing manga")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Manga ou relacionamentos não encontrados")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Atualize os dados. Mande o ID do autor para manter/trocar o vínculo.",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "title": "Berserk - Edição de Luxo",
                              "sinopsis": "A nova jornada de Guts...",
                              "status": "FINALIZADO",
                              "author": { "id": 1 },
                              "genres": [
                                { "id": 1 },
                                { "id": 3 }
                              ],
                              "details": {
                                "isbn": "978-85-XXXX-XX",
                                "licensed": true,
                                "publicationYear": 1989
                              }
                            }
                            """)))
    @PutMapping("/{id}")
    public ResponseEntity<Manga> updateManga(@PathVariable long id, @Valid @RequestBody Manga updatedManga){
        return mangaRepository.findById(id).map(manga -> {

            // Atualiza dados básicos
            manga.setTitle(updatedManga.getTitle());
            manga.setSinopsis(updatedManga.getSinopsis());
            manga.setStatus(updatedManga.getStatus());

            // Valida e atualiza Autor
            if (updatedManga.getAuthor() != null && updatedManga.getAuthor().getId() != null) {
                Author author = authorRepository.findById(updatedManga.getAuthor().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autor não encontrado"));
                manga.setAuthor(author);
            }

            // Valida e atualiza Gêneros
            if (updatedManga.getGenres() != null) {
                List<Genre> fetchedGenres = new ArrayList<>();
                for (Genre g : updatedManga.getGenres()) {
                    Genre foundGenre = genreRepository.findById(g.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gênero não encontrado: ID " + g.getId()));
                    fetchedGenres.add(foundGenre);
                }
                manga.setGenres(fetchedGenres);
            }

            if(updatedManga.getDetails() != null){
                if(manga.getDetails() != null){
                    manga.getDetails().setIsbn(updatedManga.getDetails().getIsbn());
                    manga.getDetails().setLicensed(updatedManga.getDetails().isLicensed());
                    manga.getDetails().setPublicationYear(updatedManga.getDetails().getPublicationYear());
                }
            }

            return ResponseEntity.ok(mangaRepository.save(manga));

        }).orElseThrow(() -> new MangaNotFoundException(id));
    }

    @Operation(summary = "Delete a manga")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Mangá não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManga(@PathVariable long id){
        if(!mangaRepository.existsById(id)){
            throw new MangaNotFoundException(id);
        }
        mangaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}