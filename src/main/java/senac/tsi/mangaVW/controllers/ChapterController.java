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
import senac.tsi.mangaVW.entities.Chapter;
import senac.tsi.mangaVW.exceptions.ChapterNotFoundException;
import senac.tsi.mangaVW.repositories.ChapterRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="chapters", description = "Chapters route")
@RestController
@RequestMapping("/chapters")
public class ChapterController {

    private final ChapterRepository chapterRepository;
    private final PagedResourcesAssembler<Chapter> pagedResourcesAssembler;

    @Autowired
    public ChapterController(ChapterRepository chapterRepository, PagedResourcesAssembler<Chapter> pagedResourcesAssembler) {
        this.chapterRepository = chapterRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all chapters paginated")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> getAllChapters(@ParameterObject Pageable pageable) {
        var chapters = chapterRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters));
    }

    @Operation(summary = "Search chapters by language")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> searchChaptersByLanguage(
            @RequestParam String language, @ParameterObject Pageable pageable) {
        var chapters = chapterRepository.findByLanguageIgnoreCase(language, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters));
    }

    @Operation(summary = "Get a single chapter by id")
    @GetMapping("/{id}")
    public EntityModel<Chapter> getChapterById(@PathVariable long id) {
        var chapter = chapterRepository.findById(id).orElseThrow(() -> new ChapterNotFoundException(id));
        return EntityModel.of(chapter,
                linkTo(methodOn(ChapterController.class).getChapterById(id)).withSelfRel(),
                linkTo(methodOn(ChapterController.class).getAllChapters(Pageable.unpaged())).withRel("chapters"));
    }

    @Operation(summary = "Create a new chapter")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Chapter> createChapter(@Valid @RequestBody Chapter newChapter) {
        Chapter savedChapter = chapterRepository.save(newChapter);
        return ResponseEntity.created(URI.create("/chapters/" + savedChapter.getId())).body(savedChapter);
    }

    @Operation(summary = "Update an existing chapter")
    @PutMapping("/{id}")
    public ResponseEntity<Chapter> updateChapter(@PathVariable long id, @Valid @RequestBody Chapter updatedChapter) {
        return chapterRepository.findById(id).map(chapter -> {
            chapter.setChapterNumber(updatedChapter.getChapterNumber());
            chapter.setLanguage(updatedChapter.getLanguage());
            chapter.setManga(updatedChapter.getManga()); // Atualiza o vínculo com o mangá
            return ResponseEntity.ok(chapterRepository.save(chapter));
        }).orElseGet(() -> {
            updatedChapter.setId(id);
            return ResponseEntity.created(URI.create("/chapters/" + id)).body(chapterRepository.save(updatedChapter));
        });
    }

    @Operation(summary = "Delete a chapter")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable long id) {
        if (!chapterRepository.existsById(id)) return ResponseEntity.notFound().build();
        chapterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}