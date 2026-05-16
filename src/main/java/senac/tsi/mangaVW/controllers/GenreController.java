package senac.tsi.mangaVW.controllers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.Genre;
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.exceptions.ApiErrorResponse;
import senac.tsi.mangaVW.exceptions.GenreNotFoundException;
import senac.tsi.mangaVW.infrastructure.RateLimit;
import senac.tsi.mangaVW.repositories.GenreRepository;
import senac.tsi.mangaVW.repositories.MangaRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
@Tag(name = "Genres", description = "Endpoints for categorizing mangas into literary genres")
@RestController
@RequestMapping("/genres")
@ApiResponse(responseCode = "400", description = "Invalid request: Bad parameters or syntax error",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "429", description = "Too Many Requests: Rate limit exceeded", content = @Content)
@ApiResponse(responseCode = "401", description = "Unauthorized: API Key is missing or invalid")
public class GenreController {
    private final GenreRepository genreRepository;
    private final PagedResourcesAssembler<Genre> pagedResourcesAssembler;
    private final MangaRepository mangaRepository; // Injetado para gerenciar o delete
    private final java.util.Map<String, IdempotentCreateResponse> createResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object createIdempotencyLock = new Object();
    private record CreateGenreFingerprint(String name) {}
    private record IdempotentCreateResponse(CreateGenreFingerprint requestFingerprint, Genre genre, URI location) {}
    @Autowired
    public GenreController(GenreRepository genreRepository,
                           PagedResourcesAssembler<Genre> pagedResourcesAssembler,
                           MangaRepository mangaRepository) {
        this.genreRepository = genreRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.mangaRepository = mangaRepository;
    }
    @Operation(summary = "Get all genres", description = "Retrieves a paginated list of all available genres in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @RateLimit()
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getAllGenres(@ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var genres = genreRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genres));
    }
    @Operation(summary = "Search genres by name", description = "Performs a case-insensitive search to find specific genres based on keywords.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search URL"),
    })
    @RateLimit(capacity = 2)
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> searchGenresByName(
            @RequestParam String name, @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var genres = genreRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genres));
    }
    @Operation(summary = "Get genre by ID", description = "Retrieves a genre by its ID. Includes HATEOAS links to manage the resource.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Genre found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Genre not found in the database")
    })
    @RateLimit(capacity = 40)
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Genre>> getGenreById(@PathVariable long id) {
        var genre = genreRepository.findById(id).orElseThrow(() -> new GenreNotFoundException(id));
        return ResponseEntity.ok(toEntityModel(genre));
    }
    @Operation(summary = "Create a new genre", description = "Registers a new genre classification. The name must be unique and properly formatted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Genre created successfully in the database"),
            @ApiResponse(responseCode = "400", description = "Invalid input provided or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with a different payload"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send only the genre name",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Romance"
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<EntityModel<Genre>> createGenre(@Valid @RequestBody Genre newGenre,
                                                          @io.swagger.v3.oas.annotations.Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
                                                          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var requestFingerprint = new CreateGenreFingerprint(newGenre.getName());
        synchronized (createIdempotencyLock) {
            var storedResponse = createResponses.get(idempotencyKey);
            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
                }
                return ResponseEntity.created(storedResponse.location())
                        .body(toEntityModel(copyOf(storedResponse.genre())));
            }
            Genre savedGenre = genreRepository.save(newGenre);
            URI location = URI.create("/genres/" + savedGenre.getId());
            createResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    copyOf(savedGenre),
                    location
            ));
            return ResponseEntity.created(location).body(toEntityModel(savedGenre));
        }
    }
    private Genre copyOf(Genre genre) {
        Genre copy = new Genre();
        copy.setId(genre.getId());
        copy.setName(genre.getName());
        return copy;
    }
    @Operation(summary = "Update a genre", description = "Modifies the name of an existing genre. Mangas previously associated with this genre will automatically reflect the new name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Genre updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided in the request"),
            @ApiResponse(responseCode = "404", description = "The genre you are trying to update does not exist"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Update the genre name. Linked mangas are not altered by this route",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Terror"
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Genre>> updateGenre(@PathVariable long id, @Valid @RequestBody Genre updatedGenre) {
        return genreRepository.findById(id).map(genre -> {
            genre.setName(updatedGenre.getName());
            Genre savedGenre = genreRepository.save(genre);
            return ResponseEntity.ok(toEntityModel(savedGenre));
        }).orElseThrow(() -> new GenreNotFoundException(id));
    }
    @Operation(summary = "Delete a genre", description = "Safely deletes a genre. Automatically unlinks the genre from any associated mangas before deletion to prevent data integrity conflicts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully!"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "The informed genre does not exist in the database"),
    })
    @RateLimit(capacity = 5, minutes = 10)
    @DeleteMapping("/{id}")
    @jakarta.transaction.Transactional
    public ResponseEntity<Void> deleteGenre(@PathVariable long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        // 1. Cria uma cópia da lista para evitar erro de concorrência (ConcurrentModificationException)
        List<Manga> mangasVinculados = new ArrayList<>(genre.getMangas());
        // 2. Remove o gênero de cada mangá e FORÇA o salvamento para atualizar a tabela intermediária
        for (Manga manga : mangasVinculados) {
            manga.getGenres().remove(genre);
            mangaRepository.save(manga);
        }
        // 3. Agora que os mangás foram salvos sem o gênero, podemos deletá-lo com segurança
        genreRepository.delete(genre);
        return ResponseEntity.noContent().build();
    }
    // --- HELPER METHOD FOR HATEOAS ---
    private EntityModel<Genre> toEntityModel(Genre genre) {
        return EntityModel.of(genre,
                linkTo(methodOn(GenreController.class).getGenreById(genre.getId())).withSelfRel(),
                linkTo(methodOn(GenreController.class).updateGenre(genre.getId(), null)).withRel("update"),
                linkTo(methodOn(GenreController.class).deleteGenre(genre.getId())).withRel("delete"),
                linkTo(methodOn(GenreController.class).getAllGenres(null)).withRel("genres"));
    }
}
