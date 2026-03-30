package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import senac.tsi.mangaVW.entities.MangaDetails;
import senac.tsi.mangaVW.exceptions.MangaDetailsNotFoundException;
import senac.tsi.mangaVW.repositories.MangaDetailsRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="manga-details", description = "Manga Details route")
@RestController
@RequestMapping("/manga-details")
public class MangaDetailsController {

    private final MangaDetailsRepository detailsRepository;
    private final PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler;

    @Autowired
    public MangaDetailsController(MangaDetailsRepository detailsRepository, PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler) {
        this.detailsRepository = detailsRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all manga details paginated")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<MangaDetails>>> getAllDetails(@ParameterObject Pageable pageable) {
        var details = detailsRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(details));
    }

    @Operation(summary = "Search only licensed manga details")
    @GetMapping("/search/licensed")
    public ResponseEntity<PagedModel<EntityModel<MangaDetails>>> getLicensedDetails(@ParameterObject Pageable pageable) {
        var details = detailsRepository.findByLicensedTrue(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(details));
    }

    @Operation(summary = "Get a single manga detail by id")
    @GetMapping("/{id}")
    public EntityModel<MangaDetails> getDetailsById(@PathVariable long id) {
        var details = detailsRepository.findById(id).orElseThrow(() -> new MangaDetailsNotFoundException(id));
        return EntityModel.of(details,
                linkTo(methodOn(MangaDetailsController.class).getDetailsById(id)).withSelfRel(),
                linkTo(methodOn(MangaDetailsController.class).getAllDetails(Pageable.unpaged())).withRel("manga-details"));
    }

    @Operation(summary = "Create new manga details")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MangaDetails> createDetails(@Valid @RequestBody MangaDetails newDetails) {
        MangaDetails savedDetails = detailsRepository.save(newDetails);
        return ResponseEntity.created(URI.create("/manga-details/" + savedDetails.getId())).body(savedDetails);
    }

    @Operation(summary = "Update existing manga details")
    @PutMapping("/{id}")
    public ResponseEntity<MangaDetails> updateDetails(@PathVariable long id, @Valid @RequestBody MangaDetails updatedDetails) {
        return detailsRepository.findById(id).map(details -> {
            details.setIsbn(updatedDetails.getIsbn());
            details.setPublicationYear(updatedDetails.getPublicationYear());
            details.setLicensed(updatedDetails.getLicensed());
            return ResponseEntity.ok(detailsRepository.save(details));
        }).orElseGet(() -> {
            updatedDetails.setId(id);
            return ResponseEntity.created(URI.create("/manga-details/" + id)).body(detailsRepository.save(updatedDetails));
        });
    }

    @Operation(summary = "Delete manga details")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDetails(@PathVariable long id) {
        if (!detailsRepository.existsById(id)) return ResponseEntity.notFound().build();
        detailsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}