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
import senac.tsi.mangaVW.entities.Chapter;
import senac.tsi.mangaVW.entities.Page;
import senac.tsi.mangaVW.exceptions.ChapterNotFoundException;
import senac.tsi.mangaVW.exceptions.PageNotFoundException;
import senac.tsi.mangaVW.repositories.ChapterRepository;
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
    private final ChapterRepository chapterRepository;

    @Autowired
    public PageController(PageRepository pageRepository, PagedResourcesAssembler<Page> pagedResourcesAssembler, ChapterRepository chapterRepository) {
        this.pageRepository = pageRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.chapterRepository = chapterRepository;
    }

    @Operation(summary = "Get all pages paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Page>>> getAllPages(@ParameterObject Pageable pageable) {
        var pages = pageRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages));
    }

    @Operation(summary = "Search pages by image URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search URL"),
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Page>>> searchPagesByUrl(
            @RequestParam String imageUrl, @ParameterObject Pageable pageable) {
        var pages = pageRepository.findByImageUrlContainingIgnoreCase(imageUrl, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages));
    }

    @Operation(summary = "Get a single page by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Page not found in the database")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Page>> getPageById(@PathVariable long id) {
        var page = pageRepository.findById(id).orElseThrow(() -> new PageNotFoundException(id));
        return ResponseEntity.ok(toEntityModel(page));
    }

    @Operation(summary = "Create a new page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Page created successfully in the database"),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or missing Chapter ID"),
            @ApiResponse(responseCode = "404", description = "The Chapter does not exist in the database"),
            @ApiResponse(responseCode = "409", description = "Conflict: Page already exists"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send page data and only the desired chapter ID",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "pageNumber": 28,
                              "imageUrl": "https://mangadex/berserk/cap10/16",
                              "chapter": {
                                "id": 1
                              }
                            }
                            """)))
    @PostMapping
    public ResponseEntity<EntityModel<Page>> createPage(@Valid @RequestBody Page newPage) {
        if(newPage.getChapter() == null || newPage.getChapter().getId() == null){
            return ResponseEntity.badRequest().build();
        }

        Chapter chapter = chapterRepository.findById(newPage.getChapter().getId())
                .orElseThrow(() -> new ChapterNotFoundException(newPage.getChapter().getId()));
        newPage.setChapter(chapter);

        Page savedPage = pageRepository.save(newPage);

        return ResponseEntity
                .created(URI.create("/pages/" + savedPage.getId()))
                .body(toEntityModel(savedPage));
    }

    @Operation(summary = "Update an existing page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided"),
            @ApiResponse(responseCode = "404", description = "The page you are trying to update does not exist")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send only the fields to be updated (chapter link is not changed here).",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "pageNumber": 30,
                              "imageUrl": "https://mangadex/berserk/cap10/16-atualizada.jpg"
                            }
                            """)))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Page>> updatePage(@PathVariable long id, @Valid @RequestBody Page updatedPage) {
        return pageRepository.findById(id).map(page -> {
            page.setPageNumber(updatedPage.getPageNumber());
            page.setImageUrl(updatedPage.getImageUrl());

            Page savedPage = pageRepository.save(page);
            return ResponseEntity.ok(toEntityModel(savedPage));
        }).orElseThrow(() -> new PageNotFoundException(id)) ;
    }

    @Operation(summary = "Delete a page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully!"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "The informed page does not exist in the database"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable long id) {
        if (!pageRepository.existsById(id))  {
            throw new PageNotFoundException(id);
        }
        pageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- HELPER METHOD FOR HATEOAS ---
    private EntityModel<Page> toEntityModel(Page page) {
        return EntityModel.of(page,
                linkTo(methodOn(PageController.class).getPageById(page.getId())).withSelfRel(),
                linkTo(methodOn(PageController.class).updatePage(page.getId(), null)).withRel("update"),
                linkTo(methodOn(PageController.class).deletePage(page.getId())).withRel("delete"),
                linkTo(methodOn(PageController.class).getAllPages(Pageable.unpaged())).withRel("pages"));
    }
}