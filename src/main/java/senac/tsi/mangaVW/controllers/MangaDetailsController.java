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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.MangaDetails;
import senac.tsi.mangaVW.exceptions.MangaDetailsNotFoundException;
import senac.tsi.mangaVW.repositories.MangaDetailsRepository;
import senac.tsi.mangaVW.repositories.MangaRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Manga Details", description = "Endpoints for managing technical publishing metadata")
@RestController
@RequestMapping("/manga-details")
public class MangaDetailsController {

    private final MangaDetailsRepository detailsRepository;
    private final PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler;
    private final MangaRepository mangaRepository; // Injetado para o Delete Seguro

    @Autowired
    public MangaDetailsController(MangaDetailsRepository detailsRepository,
                                  PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler,
                                  MangaRepository mangaRepository) {
        this.detailsRepository = detailsRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.mangaRepository = mangaRepository;
    }

    @Operation(summary = "Get all manga details", description = "Retrieves a paginated list of all technical metadata records (ISBN, publication year, etc.).")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<MangaDetails>>> getAllDetails(@ParameterObject Pageable pageable) {
        var details = detailsRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(details));
    }

    @Operation(summary = "Search by license status", description = "Filters the technical details based on their official licensing status (true for licensed, false for unlicensed).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<MangaDetails>>> searchDetailsByLicense(
            @RequestParam Boolean licensed, @ParameterObject Pageable pageable) {
        var details = detailsRepository.findByLicensed(licensed, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(details));
    }

    @Operation(summary = "Get details by ID", description = "Retrieves a specific technical details record using its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga details found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Manga details ID does not exist")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<MangaDetails>> getDetailsById(@PathVariable long id) {
        var details = detailsRepository.findById(id)
                .orElseThrow(() -> new MangaDetailsNotFoundException(id));

        return ResponseEntity.ok(toEntityModel(details));
    }

    @Operation(summary = "Create manga details", description = "Creates a standalone technical details record which can later be attached to a manga entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manga details created successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed JSON"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
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
    public ResponseEntity<EntityModel<MangaDetails>> createDetails(@Valid @RequestBody MangaDetails newDetails) {
        MangaDetails savedDetails = detailsRepository.save(newDetails);

        return ResponseEntity
                .created(URI.create("/manga-details/" + savedDetails.getId()))
                .body(toEntityModel(savedDetails));
    }

    @Operation(summary = "Update manga details", description = "Updates the ISBN, publication year, or licensing status of an existing details record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manga details updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided"),
            @ApiResponse(responseCode = "404", description = "Manga details not found")
    })
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
            @ApiResponse(responseCode = "404", description = "Manga details not found")
    })
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
                linkTo(methodOn(MangaDetailsController.class).getAllDetails(Pageable.unpaged())).withRel("all-details"));
    }
}