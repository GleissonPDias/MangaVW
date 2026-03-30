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

// IMPORTANTE: Importando a SUA entidade Page
import senac.tsi.mangaVW.entities.Page;
import senac.tsi.mangaVW.exceptions.PageNotFoundException;
import senac.tsi.mangaVW.repositories.PageRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="pages", description = "Pages route")
@RestController
@RequestMapping("/pages")
public class PageController {

    private final PageRepository pageRepository;
    private final PagedResourcesAssembler<Page> pagedResourcesAssembler;

    @Autowired
    public PageController(PageRepository pageRepository, PagedResourcesAssembler<Page> pagedResourcesAssembler) {
        this.pageRepository = pageRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all pages paginated")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Page>>> getAllPages(@ParameterObject Pageable pageable) {
        var pages = pageRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages));
    }

    @Operation(summary = "Search pages by image URL")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Page>>> searchPagesByUrl(
            @RequestParam String imageUrl, @ParameterObject Pageable pageable) {
        var pages = pageRepository.findByImageUrlContainingIgnoreCase(imageUrl, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages));
    }

    @Operation(summary = "Get a single page by id")
    @GetMapping("/{id}")
    public EntityModel<Page> getPageById(@PathVariable long id) {
        var page = pageRepository.findById(id).orElseThrow(() -> new PageNotFoundException(id));
        return EntityModel.of(page,
                linkTo(methodOn(PageController.class).getPageById(id)).withSelfRel(),
                linkTo(methodOn(PageController.class).getAllPages(Pageable.unpaged())).withRel("pages"));
    }

    @Operation(summary = "Create a new page")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Page> createPage(@Valid @RequestBody Page newPage) {
        Page savedPage = pageRepository.save(newPage);
        return ResponseEntity.created(URI.create("/pages/" + savedPage.getId())).body(savedPage);
    }

    @Operation(summary = "Update an existing page")
    @PutMapping("/{id}")
    public ResponseEntity<Page> updatePage(@PathVariable long id, @Valid @RequestBody Page updatedPage) {
        return pageRepository.findById(id).map(page -> {
            page.setPageNumber(updatedPage.getPageNumber());
            page.setImageUrl(updatedPage.getImageUrl());
            page.setChapter(updatedPage.getChapter()); // Atualiza o vínculo com o capítulo
            return ResponseEntity.ok(pageRepository.save(page));
        }).orElseGet(() -> {
            updatedPage.setId(id);
            return ResponseEntity.created(URI.create("/pages/" + id)).body(pageRepository.save(updatedPage));
        });
    }

    @Operation(summary = "Delete a page")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable long id) {
        if (!pageRepository.existsById(id)) return ResponseEntity.notFound().build();
        pageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}