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
import senac.tsi.mangaVW.entities.Chapter;
import senac.tsi.mangaVW.entities.Page;
import senac.tsi.mangaVW.exceptions.ApiErrorResponse;
import senac.tsi.mangaVW.exceptions.ChapterNotFoundException;
import senac.tsi.mangaVW.exceptions.PageNotFoundException;
import senac.tsi.mangaVW.infrastructure.RateLimit;
import senac.tsi.mangaVW.repositories.ChapterRepository;
import senac.tsi.mangaVW.repositories.PageRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Pages", description = "Endpoints for managing individual reading pages and image URLs")
@RestController
@RequestMapping("/pages")
// 🛡️ Documentação global de erro 400 para todos os métodos da classe
@ApiResponse(responseCode = "400", description = "Invalid request: Bad parameters or malformed JSON",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "429", description = "Too Many Requests: Rate limit exceeded", content = @Content)
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

    @Operation(summary = "Get all pages", description = "Retrieves a paginated list of all reading pages.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully")
    })
    @RateLimit(capacity = 20, minutes = 1)
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Page>>> getAllPages(@ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var pages = pageRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages, this::toEntityModel));
    }

    @Operation(summary = "Search pages by image URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully")

    })
    @RateLimit(capacity = 2)
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Page>>> searchPagesByUrl(
            @RequestParam String imageUrl, @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var pages = pageRepository.findByImageUrlContainingIgnoreCase(imageUrl, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages, this::toEntityModel));
    }

    @Operation(summary = "Get page by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page found successfully"),
            @ApiResponse(responseCode = "404", description = "Page not found")
    })
    @RateLimit(capacity = 40)
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Page>> getPageById(@PathVariable long id) {
        var page = pageRepository.findById(id).orElseThrow(() -> new PageNotFoundException(id));
        return ResponseEntity.ok(toEntityModel(page));
    }

    @Operation(summary = "Create a new page", description = "Adds a new page to an existing chapter. The full hierarchy is simulated in the example to pass validation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Page created successfully"),
            @ApiResponse(responseCode = "404", description = "The Chapter does not exist"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send the new page data. The chapter ID is required.",
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
        return ResponseEntity.created(URI.create("/pages/" + savedPage.getId())).body(toEntityModel(savedPage));
    }

    @Operation(summary = "Update a page", description = "Updates the page number or image URL. The chapter link is immutable.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page updated successfully"),
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "pageNumber": 30,
                              "imageUrl": "https://mangadex/berserk/cap10/16-atualizada.jpg",
                              "chapter": {
                                "id": 2
                              }
                            }
                            """)))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Page>> updatePage(@PathVariable long id, @Valid @RequestBody Page updatedPage) {
        return pageRepository.findById(id).map(page -> {
            page.setPageNumber(updatedPage.getPageNumber());
            page.setImageUrl(updatedPage.getImageUrl());

            if(updatedPage.getChapter() != null && updatedPage.getChapter().getId() != null) {
                Chapter newChapter = chapterRepository.findById(updatedPage.getChapter().getId())
                .orElseThrow(() -> new ChapterNotFoundException(updatedPage.getChapter().getId()));
                page.setChapter(newChapter);
            }
            Page savedPage = pageRepository.save(page);
            return ResponseEntity.ok(toEntityModel(savedPage));
        }).orElseThrow(() -> new PageNotFoundException(id)) ;
    }

    @Operation(summary = "Delete a page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Page not found")
    })
    @RateLimit(capacity = 5, minutes = 10)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable long id) {
        if (!pageRepository.existsById(id))  {
            throw new PageNotFoundException(id);
        }
        pageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<Page> toEntityModel(Page page) {
        return EntityModel.of(page,
                linkTo(methodOn(PageController.class).getPageById(page.getId())).withSelfRel(),
                linkTo(methodOn(PageController.class).updatePage(page.getId(), null)).withRel("update"),
                linkTo(methodOn(PageController.class).deletePage(page.getId())).withRel("delete"),
                linkTo(methodOn(PageController.class).getAllPages(null)).withRel("pages"),
                linkTo(methodOn(ChapterController.class).getChapterById(page.getChapter().getId())).withRel("parent_chapter"));
    }
}