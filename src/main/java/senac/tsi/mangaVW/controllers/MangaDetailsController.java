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
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.MangaDetails;
import senac.tsi.mangaVW.exceptions.ApiErrorResponse;
import senac.tsi.mangaVW.exceptions.MangaDetailsNotFoundException;
import senac.tsi.mangaVW.infrastructure.RateLimit;
import senac.tsi.mangaVW.repositories.MangaDetailsRepository;
import senac.tsi.mangaVW.repositories.MangaRepository;
import java.net.URI;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
@Tag(name = "Manga Details", description = "Endpoints for managing technical publishing metadata")
@RestController
@RequestMapping("/manga-details")
@ApiResponse(responseCode = "400", description = "Invalid request: Bad parameters or syntax error",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "429", description = "Too Many Requests: Rate limit exceeded", content = @Content)
@ApiResponse(responseCode = "401", description = "Unauthorized: API Key is missing or invalid")
public class MangaDetailsController {
    private final MangaDetailsRepository detailsRepository;
    private final PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler;
    private final MangaRepository mangaRepository;
    private final java.util.Map<String, IdempotentCreateResponse> createResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object createIdempotencyLock = new Object();
    private record CreateDetailsFingerprint(String isbn, Integer publicationYear, boolean licensed) {}
    private record IdempotentCreateResponse(CreateDetailsFingerprint requestFingerprint, MangaDetails details, URI location) {}
    @Autowired
    public MangaDetailsController(MangaDetailsRepository detailsRepository,
                                  PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler,
                                  MangaRepository mangaRepository) {
        this.detailsRepository = detailsRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.mangaRepository = mangaRepository;
    }
    @Operation(summary = "Get all manga details", description = "Retrieves a paginated list of all technical metadata records. You can optionally filter by licensing status (true/false).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search/List completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
    })
    @RateLimit()
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<MangaDetails>>> getAllDetails(
            @RequestParam(required = false) Boolean licensed,
            @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        org.springframework.data.domain.Page<MangaDetails> details;
        // Se o front-end enviou ?licensed=true ou false, usamos o filtro
        if (licensed != null) {
            details = detailsRepository.findByLicensed(licensed, pageable);
        }
        // Se não enviou nada, buscamos todos os detalhes
        else {
            details = detailsRepository.findAll(pageable);
        }
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(details, this::toEntityModel));
    }
    @Operation(summary = "Get details by ID", description = "Retrieves a specific technical details record using its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga details found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Manga details ID does not exist")
    })
    @RateLimit(capacity = 40)
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<MangaDetails>> getDetailsById(@PathVariable long id) {
        var details = detailsRepository.findById(id)
                .orElseThrow(() -> new MangaDetailsNotFoundException(id));
        return ResponseEntity.ok(toEntityModel(details));
    }
    @Operation(summary = "Create manga details", description = "Creates a standalone technical details record which can later be attached to a manga entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manga details created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input provided or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with a different payload"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send the technical data of the manga",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "isbn": "978-85-XXXX-XX",
                              "publicationYear": 2026,
                              "licensed": true
                            }
                            """)))
    @PostMapping
    public ResponseEntity<EntityModel<MangaDetails>> createDetails(@Valid @RequestBody MangaDetails newDetails,
                                                                   @io.swagger.v3.oas.annotations.Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
                                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var requestFingerprint = new CreateDetailsFingerprint(newDetails.getIsbn(), newDetails.getPublicationYear(), newDetails.isLicensed());
        synchronized (createIdempotencyLock) {
            var storedResponse = createResponses.get(idempotencyKey);
            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
                }
                return ResponseEntity.created(storedResponse.location())
                        .body(toEntityModel(copyOf(storedResponse.details())));
            }
            MangaDetails savedDetails = detailsRepository.save(newDetails);
            URI location = URI.create("/manga-details/" + savedDetails.getId());
            createResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    copyOf(savedDetails),
                    location
            ));
            return ResponseEntity.created(location).body(toEntityModel(savedDetails));
        }
    }
    private MangaDetails copyOf(MangaDetails details) {
        MangaDetails copy = new MangaDetails();
        copy.setId(details.getId());
        copy.setIsbn(details.getIsbn());
        copy.setPublicationYear(details.getPublicationYear());
        copy.setLicensed(details.isLicensed());
        return copy;
    }
    @Operation(summary = "Update manga details", description = "Updates the ISBN, publication year, or licensing status of an existing details record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga details updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided"),
            @ApiResponse(responseCode = "404", description = "Manga details not found"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Update the ISBN, Publication Year, or Licensed status",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "isbn": "123-456-789",
                              "publicationYear": 1989,
                              "licensed": false
                            }
                            """)))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<MangaDetails>> updateDetails(@PathVariable long id, @Valid @RequestBody MangaDetails updatedDetails) {
        return detailsRepository.findById(id).map(details -> {
            details.setIsbn(updatedDetails.getIsbn());
            details.setPublicationYear(updatedDetails.getPublicationYear());
            details.setLicensed(updatedDetails.isLicensed());
            MangaDetails savedDetails = detailsRepository.save(details);
            return ResponseEntity.ok(toEntityModel(savedDetails));
        }).orElseThrow(() -> new MangaDetailsNotFoundException(id));
    }
    @Operation(summary = "Delete manga details", description = "Safely deletes a details record. If currently linked to a manga, the relationship is automatically severed before deletion to avoid conflicts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Manga details not found")
    })
    @RateLimit(capacity = 5, minutes = 10)
    @DeleteMapping("/{id}")
    @jakarta.transaction.Transactional // Garante a exclusão segura
    public ResponseEntity<Void> deleteDetails(@PathVariable long id) {
        MangaDetails details = detailsRepository.findById(id)
                .orElseThrow(() -> new MangaDetailsNotFoundException(id));
        // Busca se existe algum mangá usando esses detalhes para desvincular antes de deletar
        mangaRepository.findByDetailsId(id).ifPresent(manga -> {
            manga.setDetails(null);
            mangaRepository.save(manga);
        });
        detailsRepository.delete(details);
        return ResponseEntity.noContent().build();
    }
    // --- HELPER METHOD FOR HATEOAS ---
    private EntityModel<MangaDetails> toEntityModel(MangaDetails details) {
        return EntityModel.of(details,
                linkTo(methodOn(MangaDetailsController.class).getDetailsById(details.getId())).withSelfRel(),
                linkTo(methodOn(MangaDetailsController.class).updateDetails(details.getId(), null)).withRel("update"),
                linkTo(methodOn(MangaDetailsController.class).deleteDetails(details.getId())).withRel("delete"),
                linkTo(methodOn(MangaDetailsController.class).getAllDetails(null, null)).withRel("all-details"));
    }
}
