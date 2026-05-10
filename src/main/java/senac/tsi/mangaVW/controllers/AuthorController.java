package senac.tsi.mangaVW.controllers;

import senac.tsi.mangaVW.infrastructure.RequireApiKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.Author;
import senac.tsi.mangaVW.exceptions.ApiErrorResponse;
import senac.tsi.mangaVW.exceptions.AuthorNotFoundException;
import senac.tsi.mangaVW.infrastructure.RateLimit;
import senac.tsi.mangaVW.repositories.AuthorRepository;

import jakarta.validation.Valid;
import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Authors", description = "Endpoints for managing manga authors and their biographical data")
@RestController
@RequestMapping("/authors")
@ApiResponse(responseCode = "400", description = "Invalid request: Bad parameters or malformed JSON",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "429", description = "Too Many Requests: Rate limit exceeded", content = @Content)
public class AuthorController {

    private final AuthorRepository authorRepository;
    private final PagedResourcesAssembler<Author> pagedResourcesAssembler;

    private final java.util.Map<String, IdempotentCreateResponse> createResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object createIdempotencyLock = new Object();

    private record CreateAuthorFingerprint(String name) {}
    private record IdempotentCreateResponse(CreateAuthorFingerprint requestFingerprint, Author author, URI location) {}

    @Autowired
    public AuthorController(AuthorRepository authorRepository,
                            PagedResourcesAssembler<Author> pagedResourcesAssembler) {
        this.authorRepository = authorRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all authors", description = "Retrieves a paginated list of all registered authors in the database. Includes embedded HATEOAS links for navigation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @RateLimit(capacity = 20, minutes = 1)
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Author>>> getAllAuthors(@ParameterObject @PageableDefault(page = 0, size = 20)  Pageable pageable) {
        var authors = authorRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors, this::toEntityModel));
    }

    @Operation(summary = "Search authors by name", description = "Performs a case-insensitive search for authors matching the provided name keyword. Returns a paginated response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search URL"),
    })
    @RateLimit(capacity = 2)
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Author>>> searchAuthorsByName(
            @RequestParam String name,
            @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var authors = authorRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors, this::toEntityModel));
    }

    @Operation(summary = "Get author by ID", description = "Retrieves the detailed profile of a specific author using their unique identifier. Includes self, update, and delete HATEOAS links.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Author not found in the database")
    })
    @RateLimit(capacity = 40)
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Author>> getAuthorById(@PathVariable long id) {
        var author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));

        // Utilizando o método auxiliar para gerar os links
        return ResponseEntity.ok(toEntityModel(author));
    }

    @Operation(summary = "Create a new author", description = "Registers a new author in the system. Requires a valid name and biography. Returns the created resource location.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Author created successfully in the database"),
            @ApiResponse(responseCode = "400", description = "Invalid input provided or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with a different payload"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send the basic data of the new author",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Masashi Kishimoto",
                                      "biography": "Renowned author, creator of the Naruto franchise."
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    @RequireApiKey
    public ResponseEntity<EntityModel<Author>> createAuthor(@Valid @RequestBody Author newAuthor,
                                                            @io.swagger.v3.oas.annotations.Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
                                                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var requestFingerprint = new CreateAuthorFingerprint(newAuthor.getName());

        synchronized (createIdempotencyLock) {
            var storedResponse = createResponses.get(idempotencyKey);

            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
                }
                return ResponseEntity.created(storedResponse.location())
                        .body(toEntityModel(copyOf(storedResponse.author())));
            }

            Author savedAuthor = authorRepository.save(newAuthor);
            URI location = URI.create("/authors/" + savedAuthor.getId());

            createResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    copyOf(savedAuthor),
                    location
            ));

            return ResponseEntity.created(location).body(toEntityModel(savedAuthor));
        }
    }

    private Author copyOf(Author author) {
        Author copy = new Author();
        copy.setId(author.getId());
        copy.setName(author.getName());
        copy.setBiography(author.getBiography());
        return copy;
    }

    @Operation(summary = "Update an author", description = "Updates the biographical information of an existing author. Linked mangas are preserved and not affected by this operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided in the request"),
            @ApiResponse(responseCode = "404", description = "The author you are trying to update does not exist"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Update author data. Linked mangas are not altered by this route.",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Masashi Kishimoto",
                                      "biography": "Creator of Naruto and Samurai 8."
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/{id}")
    @RequireApiKey
    public ResponseEntity<EntityModel<Author>> updateAuthor(@PathVariable long id, @Valid @RequestBody Author updatedAuthor) {
        return authorRepository.findById(id).map(author -> {
            author.setName(updatedAuthor.getName());
            author.setBiography(updatedAuthor.getBiography());

            Author savedAuthor = authorRepository.save(author);

            // Retorna o 200 OK junto com o EntityModel contendo os links atualizados
            return ResponseEntity.ok(toEntityModel(savedAuthor));

        }).orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @Operation(summary = "Delete an author", description = "Permanently removes an author from the database. Due to database constraints, associated entities may be affected based on cascade rules.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Author deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @RateLimit(capacity = 5, minutes = 10)
    @DeleteMapping("/{id}")
    @RequireApiKey
    public ResponseEntity<Void> deleteAuthor(@PathVariable long id) {
        if (!authorRepository.existsById(id)) {
            throw new AuthorNotFoundException(id);
        }
        authorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    // Centraliza a criação dos links, evitando repetição de código no GET, POST e PUT
    private EntityModel<Author> toEntityModel(Author author) {
        return EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthorById(author.getId())).withSelfRel(),
                linkTo(methodOn(AuthorController.class).updateAuthor(author.getId(), null)).withRel("update"),
                linkTo(methodOn(AuthorController.class).deleteAuthor(author.getId())).withRel("delete"),
                linkTo(methodOn(AuthorController.class).getAllAuthors(null)).withRel("authors"));
    }
}
