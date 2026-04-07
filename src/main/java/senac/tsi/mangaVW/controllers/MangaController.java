package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

@Tag(name = "Mangas", description = "Core endpoints for managing the manga catalog and synchronization")
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

    @Operation(summary = "Search mangas by title", description = "Performs a paginated, case-insensitive search for mangas containing the specified title keyword.")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Manga>>> searchMangasByTitle(
            @RequestParam String title,
            @ParameterObject Pageable pageable) {

        var mangas = mangaRepository.findByTitleContainingIgnoreCase(title, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(mangas));
    }

    @Operation(summary = "Sync mangas from MangaDex API", description = "Triggers an automated background process to fetch external manga data (titles, authors, cover arts, genres) from the public MangaDex API and saves them to the local database.")
    @PostMapping("/sync")
    public ResponseEntity<String> syncFromMangaDex() {
        try {
            mangaDexService.syncMangasFromMangaDex();
            return ResponseEntity.ok("Sync with MangaDex completed successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing with MangaDex: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all mangas", description = "Retrieves a comprehensive, paginated list of all mangas available in the catalog, including their related entities (Author, Details).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Manga>>> getAllMangas(@ParameterObject Pageable pageable){
        var mangas = mangaRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(mangas));
    }

    @Operation(summary = "Get manga by ID", description = "Retrieves full details of a specific manga by its unique ID. The response is enriched with HATEOAS links for resource discoverability.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Manga not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Manga>> getMangaById(@PathVariable long id){
        var manga = mangaRepository.findById(id)
                .orElseThrow(() -> new MangaNotFoundException(id));

        return ResponseEntity.ok(toEntityModel(manga));
    }

    @Operation(summary = "Create a new manga", description = "Adds a new manga to the catalog. Requires an existing Author ID. Genres and technical details can be optionally linked during creation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manga created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data provided"),
            @ApiResponse(responseCode = "404", description = "Author or Genre provided does not exist")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Manga data. Author ID is mandatory. Genres and Details are optional.",
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
    public ResponseEntity<EntityModel<Manga>> createManga(@Valid @RequestBody Manga newManga){

        // 1. Busca e valida o Autor
        if (newManga.getAuthor() == null || newManga.getAuthor().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Author author = authorRepository.findById(newManga.getAuthor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found"));
        newManga.setAuthor(author);

        // 2. Busca e valida a lista de Gêneros (se for enviada)
        if (newManga.getGenres() != null && !newManga.getGenres().isEmpty()) {
            List<Genre> fetchedGenres = new ArrayList<>();
            for (Genre g : newManga.getGenres()) {

                if(g.getId() == null){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genre ID cannot be null");
                }

                Genre foundGenre = genreRepository.findById(g.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre not found: ID " + g.getId()));
                fetchedGenres.add(foundGenre);
            }
            newManga.setGenres(fetchedGenres);
        }

        Manga savedManga = mangaRepository.save(newManga);
        return ResponseEntity
                .created(URI.create("/mangas/"+ savedManga.getId()))
                .body(toEntityModel(savedManga));
    }

    @Operation(summary = "Update a manga", description = "Updates the core metadata of an existing manga. Relationships with authors, genres, and details can be modified by providing their respective IDs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Manga or relationships not found")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Update the data. Send the author ID to maintain/change the link.",
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
    public ResponseEntity<EntityModel<Manga>> updateManga(@PathVariable long id, @Valid @RequestBody Manga updatedManga){
        return mangaRepository.findById(id).map(manga -> {

            // Atualiza dados básicos
            manga.setTitle(updatedManga.getTitle());
            manga.setSinopsis(updatedManga.getSinopsis());
            manga.setStatus(updatedManga.getStatus());

            // Valida e atualiza Autor
            if (updatedManga.getAuthor() != null && updatedManga.getAuthor().getId() != null) {
                Author author = authorRepository.findById(updatedManga.getAuthor().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found"));
                manga.setAuthor(author);
            }

            // Valida e atualiza Gêneros
            if (updatedManga.getGenres() != null) {
                List<Genre> fetchedGenres = new ArrayList<>();
                for (Genre g : updatedManga.getGenres()) {

                    if(g.getId() == null){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genre ID cannot be null");
                    }

                    Genre foundGenre = genreRepository.findById(g.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre not found: ID " + g.getId()));
                    fetchedGenres.add(foundGenre);
                }
                manga.setGenres(fetchedGenres);
            }

            if(updatedManga.getDetails() != null){
                if(manga.getDetails() != null){
                    // Se o mangá já tem detalhes, apenas atualiza os valores
                    manga.getDetails().setIsbn(updatedManga.getDetails().getIsbn());
                    manga.getDetails().setLicensed(updatedManga.getDetails().isLicensed());
                    manga.getDetails().setPublicationYear(updatedManga.getDetails().getPublicationYear());
                } else {
                    // Se o mangá NÃO tem detalhes no banco, vincula os novos detalhes!
                    manga.setDetails(updatedManga.getDetails());
                }
            }

            Manga savedManga = mangaRepository.save(manga);
            return ResponseEntity.ok(toEntityModel(savedManga));

        }).orElseThrow(() -> new MangaNotFoundException(id));
    }

    @Operation(summary = "Delete a manga", description = "Permanently deletes a manga. All associated chapters, pages, and technical details will be cascaded and deleted automatically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Manga not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManga(@PathVariable long id){
        if(!mangaRepository.existsById(id)){
            throw new MangaNotFoundException(id);
        }
        mangaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- HELPER METHOD FOR HATEOAS ---
    private EntityModel<Manga> toEntityModel(Manga manga) {
        return EntityModel.of(manga,
                linkTo(methodOn(MangaController.class).getMangaById(manga.getId())).withSelfRel(),
                linkTo(methodOn(MangaController.class).updateManga(manga.getId(), null)).withRel("update"),
                linkTo(methodOn(MangaController.class).deleteManga(manga.getId())).withRel("delete"),
                linkTo(methodOn(MangaController.class).getAllMangas(Pageable.unpaged())).withRel("mangas"));
    }
}