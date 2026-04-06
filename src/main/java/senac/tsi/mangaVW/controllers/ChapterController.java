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
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.exceptions.ChapterNotFoundException;
import senac.tsi.mangaVW.exceptions.MangaNotFoundException;
import senac.tsi.mangaVW.repositories.ChapterRepository;
import senac.tsi.mangaVW.repositories.MangaRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="chapters", description = "Chapters route")
@RestController
@RequestMapping("/chapters")
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

    @Operation(summary = "Get all chapters paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> getAllChapters(@ParameterObject Pageable pageable) {
        var chapters = chapterRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters));
    }

    @Operation(summary = "Search chapters by language")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search URL"),
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> searchChaptersByLanguage(
            @RequestParam String language, @ParameterObject Pageable pageable) {
        var chapters = chapterRepository.findByLanguageIgnoreCase(language, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters));
    }

    @Operation(summary = "Get a single chapter by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Chapter not found in the database")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Chapter>> getChapterById(@PathVariable long id) {
        var chapter = chapterRepository.findById(id).orElseThrow(() -> new ChapterNotFoundException(id));

        // Utilizando o método auxiliar para HATEOAS
        return ResponseEntity.ok(toEntityModel(chapter));
    }

    @Operation(summary = "Create a new chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter created successfully in the database"),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or missing Manga ID"),
            @ApiResponse(responseCode = "404", description = "The provided Manga does not exist in the database"),
            @ApiResponse(responseCode = "409", description = "Conflict: the chapter already exists in this manga"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity: Field validation error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send chapter data and only the desired manga ID",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "chapterNumber": 10.5,
                                      "language": "pt-br",
                                      "manga":{
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

    @Operation(summary = "Update an existing chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON provided in the request"),
            @ApiResponse(responseCode = "404", description = "The chapter you are trying to update does not exist")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Send only the chapter fields to be updated (the link with the manga is not changed here).",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                       "chapterNumber": 1,
                                       "language": "pt-br"
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

            Chapter savedChapter = chapterRepository.save(chapter);

            // Retorna o 200 OK com HATEOAS
            return ResponseEntity.ok(toEntityModel(savedChapter));
        }).orElseThrow(() -> new ChapterNotFoundException(id));
    }

    @Operation(summary = "Delete a chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully!"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "The informed chapter does not exist in the database"),
    })
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
                linkTo(methodOn(ChapterController.class).getAllChapters(Pageable.unpaged())).withRel("chapters"));
    }
}