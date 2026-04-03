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

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="manga-details", description = "Operações técnicas e detalhes de publicação")
@RestController
@RequestMapping("/manga-details")
public class MangaDetailsController {

    private final MangaDetailsRepository detailsRepository;
    private final PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler;

    @Autowired
    public MangaDetailsController(MangaDetailsRepository detailsRepository,
                                  PagedResourcesAssembler<MangaDetails> pagedResourcesAssembler) {
        this.detailsRepository = detailsRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Listar todos os detalhes técnicos (paginado)")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<MangaDetails>>> getAllDetails(@ParameterObject Pageable pageable) {
        var details = detailsRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(details));
    }

    @Operation(summary = "Buscar detalhes por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes encontrados"),
            @ApiResponse(responseCode = "404", description = "ID de detalhes não existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<MangaDetails>> getDetailsById(@PathVariable long id) {
        var details = detailsRepository.findById(id)
                .orElseThrow(() -> new MangaDetailsNotFoundException(id));

        var entityModel = EntityModel.of(details,
                linkTo(methodOn(MangaDetailsController.class).getDetailsById(id)).withSelfRel(),
                linkTo(methodOn(MangaDetailsController.class).getAllDetails(Pageable.unpaged())).withRel("all-details"));

        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Criar novos detalhes técnicos")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie os dados técnicos da obra",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "isbn": "978-85-XXXX-XX",
                              "publicationYear": 2026,
                              "licensed": true
                            }
                            """)))
    @PostMapping
    public ResponseEntity<MangaDetails> createDetails(@Valid @RequestBody MangaDetails newDetails) {
        MangaDetails savedDetails = detailsRepository.save(newDetails);
        return ResponseEntity.created(URI.create("/manga-details/" + savedDetails.getId())).body(savedDetails);
    }

    @Operation(summary = "Atualizar detalhes existentes")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Atualize os campos de ISBN, Ano ou Licenciamento",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "isbn": "123-456-789",
                              "publicationYear": 1989,
                              "licensed": false
                            }
                            """)))
    @PutMapping("/{id}")
    public ResponseEntity<MangaDetails> updateDetails(@PathVariable long id, @Valid @RequestBody MangaDetails updatedDetails) {
        return detailsRepository.findById(id).map(details -> {
            details.setIsbn(updatedDetails.getIsbn());
            details.setPublicationYear(updatedDetails.getPublicationYear());
            details.setLicensed(updatedDetails.isLicensed());
            return ResponseEntity.ok(detailsRepository.save(details));
        }).orElseThrow(() -> new MangaDetailsNotFoundException(id));
    }

    @Operation(summary = "Remover detalhes")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDetails(@PathVariable long id) {
        if (!detailsRepository.existsById(id)) {
            throw new MangaDetailsNotFoundException(id);
        }
        detailsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}