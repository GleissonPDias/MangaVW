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
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.exceptions.ApiErrorResponse;
import senac.tsi.mangaVW.exceptions.ChapterNotFoundException;
import senac.tsi.mangaVW.exceptions.MangaNotFoundException;
import senac.tsi.mangaVW.infrastructure.RateLimit;
import senac.tsi.mangaVW.repositories.ChapterRepository;
import senac.tsi.mangaVW.repositories.MangaRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Chapters", description = "Endpoints for managing the episodic chapter structure of mangas")
@RestController
@RequestMapping("/chapters")
@ApiResponse(responseCode = "400", description = "Invalid request: Bad parameters or malformed JSON",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "429", description = "Too Many Requests: Rate limit exceeded", content = @Content)
public class ChapterController {

    private final ChapterRepository chapterRepository;
    private final PagedResourcesAssembler<Chapter> pagedResourcesAssembler;
    private final MangaRepository mangaRepository;

    @Autowired
    public ChapterController(ChapterRepository chapterRepository, PagedResourcesAssembler<Chapter> pagedResourcesAssembler, MangaRepository mangaRepository) {
        this.chapterRepository = chapterRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.mangaRepository = mangaRepository;
    }

    @Operation(summary = "Get all chapters", description = "Retrieves a paginated list of all chapters across all mangas in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @RateLimit()
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> getAllChapters(@ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var chapters = chapterRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters, this::toEntityModel));
    }

    @Operation(summary = "Search chapters by language", description = "Filters and returns a paginated list of chapters that match the specified language code (e.g., 'en', 'pt-br').")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search URL"),
    })
    @RateLimit(capacity = 2)
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> searchChaptersByLanguage(
            @RequestParam String language, @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        var chapters = chapterRepository.findByLanguageIgnoreCase(language, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters, this::toEntityModel));
    }

    @Operation(summary = "Get chapter by ID", description = "Retrieves a specific chapter's information by its unique identifier, enabling access to its associated reading pages.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Chapter not found in the database")
    })
    @RateLimit(capacity = 40)
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Chapter>> getChapterById(@PathVariable long id) {
        var chapter = chapterRepository.findById(id).orElseThrow(() -> new ChapterNotFoundException(id));
        return ResponseEntity.ok(toEntityModel(chapter));
    }

    @Operation(summary = "Create a new chapter", description = "Registers a new chapter and links it to an existing manga. The manga ID must be provided in the request body.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter created successfully in the database"),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or missing Manga ID"),
            @ApiResponse(responseCode = "404", description = "The provided Manga does not exist in the database"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send chapter data and only the desired manga ID",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "chapterNumber": 10.5,
                                      "language": "pt-br",
                                      "manga": {
                                        "id": 1
                                      }
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<EntityModel<Chapter>> createChapter(@Valid @RequestBody Chapter newChapter) {
        if(newChapter.getManga() == null || newChapter.getManga().getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Manga manga = mangaRepository.findById(newChapter.getManga().getId())
                .orElseThrow(() -> new MangaNotFoundException(newChapter.getManga().getId()));
        newChapter.setManga(manga);

        Chapter savedChapter = chapterRepository.save(newChapter);

        // Retorna o 201 Created junto com o EntityModel contendo os links (e a URI corrigida para plural)
        return ResponseEntity
                .created(URI.create("/chapters/" + savedChapter.getId()))
                .body(toEntityModel(savedChapter));
    }

    @Operation(summary = "Update a chapter", description = "Updates the chapter number or language. The relationship to its parent manga remains unchanged.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided in the request"),
            @ApiResponse(responseCode = "404", description = "The chapter you are trying to update does not exist"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @RateLimit(capacity = 10, minutes = 5)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send only the chapter fields to be updated. Optionally send the Manga ID to transfer this chapter.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "chapterNumber": 10.5,
                                      "language": "pt-br",
                                      "manga": {
                                        "id": 1
                                      }
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Chapter>> updateChapter(@PathVariable long id, @Valid @RequestBody Chapter updatedChapter) {
        return chapterRepository.findById(id).map(chapter -> {
            chapter.setChapterNumber(updatedChapter.getChapterNumber());
            chapter.setLanguage(updatedChapter.getLanguage());

            if (updatedChapter.getManga() != null && updatedChapter.getManga().getId() != null) {
                Manga newManga = mangaRepository.findById(updatedChapter.getManga().getId())
                        .orElseThrow(() -> new MangaNotFoundException(updatedChapter.getManga().getId()));
                chapter.setManga(newManga);
            }

            Chapter savedChapter = chapterRepository.save(chapter);

            // Retorna o 200 OK com HATEOAS
            return ResponseEntity.ok(toEntityModel(savedChapter));
        }).orElseThrow(() -> new ChapterNotFoundException(id));
    }

    @Operation(summary = "Delete a chapter", description = "Deletes a chapter from the database. Any pages exclusively associated with this chapter will be deleted in cascade.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully!"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "The informed chapter does not exist in the database"),
    })
    @RateLimit(capacity = 5, minutes = 10)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable long id) {
        if (!chapterRepository.existsById(id)) {
            throw new ChapterNotFoundException(id);
        }
        chapterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- MÉTODO AUXILIAR PARA HATEOAS ---
    private EntityModel<Chapter> toEntityModel(Chapter chapter) {
        return EntityModel.of(chapter,
                linkTo(methodOn(ChapterController.class).getChapterById(chapter.getId())).withSelfRel(),
                linkTo(methodOn(ChapterController.class).updateChapter(chapter.getId(), null)).withRel("update"),
                linkTo(methodOn(ChapterController.class).deleteChapter(chapter.getId())).withRel("delete"),
                linkTo(methodOn(ChapterController.class).getAllChapters(null)).withRel("chapters"),
                linkTo(methodOn(MangaController.class).getMangaById(chapter.getManga().getId())).withRel("parent_manga"));
    }
}