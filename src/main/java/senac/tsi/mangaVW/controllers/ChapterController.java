package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.HttpStatus;
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
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> getAllChapters(@ParameterObject Pageable pageable) {
        var chapters = chapterRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters));
    }


    @Operation(summary = "Search chapters by language")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pesquisa realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "URL de pesquisa inválida"),
    })


    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Chapter>>> searchChaptersByLanguage(
            @RequestParam String language, @ParameterObject Pageable pageable) {
        var chapters = chapterRepository.findByLanguageIgnoreCase(language, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(chapters));
    }

    @Operation(summary = "Get a single chapter by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Capítulo encontrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "Página não encontrada no banco de dados")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Chapter>> getChapterById(@PathVariable long id) {
        var chapter = chapterRepository.findById(id).orElseThrow(() -> new ChapterNotFoundException(id));
        var entityModel = EntityModel.of(chapter,
                linkTo(methodOn(ChapterController.class).getChapterById(id)).withSelfRel(),
                linkTo(methodOn(ChapterController.class).getAllChapters(Pageable.unpaged())).withRel("chapters"));
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter criada com sucesso no banco de dados"),
            @ApiResponse(responseCode = "400", description = "JSON mal formatado ou faltando o ID do Mangá"),
            @ApiResponse(responseCode = "404", description = "O Mangá informado não existe no banco de dados"),
            @ApiResponse(responseCode = "409", description = "Conflito: o capítulo já existe neste mangá"),
            @ApiResponse(responseCode = "422", description = "Entidade não processável: Erro de validação dos campos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie os dados da capítulo e apenas o ID do mangá desejado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
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
    public ResponseEntity<Chapter> createChapter(@Valid @RequestBody Chapter newChapter) {
        if(newChapter.getManga() == null || newChapter.getManga().getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Manga manga = mangaRepository.findById(newChapter.getManga().getId())
                .orElseThrow(() -> new MangaNotFoundException(newChapter.getManga().getId()));
        newChapter.setManga(manga);

        Chapter savedChapter = chapterRepository.save(newChapter);

        return ResponseEntity
                .created(URI.create("/chapter/" + savedChapter.getId()))
                .body(savedChapter);
    }



    @Operation(summary = "Update an existing chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Capítulo atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "JSON inválido fornecido na requisição"),
            @ApiResponse(responseCode = "404", description = "O capítulo que você está tentando atualizar não existe")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie apenas os campos da página que serão atualizados (o vínculo com o mangá não é alterado por aqui).",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
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
    public ResponseEntity<Chapter> updateChapter(@PathVariable long id, @Valid @RequestBody Chapter updatedChapter) {
        return chapterRepository.findById(id).map(chapter -> {
            chapter.setChapterNumber(updatedChapter.getChapterNumber());
            chapter.setLanguage(updatedChapter.getLanguage());
            return ResponseEntity.ok(chapterRepository.save(chapter));
        }).orElseThrow(() -> new ChapterNotFoundException(id));

    }

    @Operation(summary = "Delete a chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deletado com sucesso!"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "O capítulo informada não existe no banco de dados"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable long id) {
        if (!chapterRepository.existsById(id)) {
            throw new ChapterNotFoundException(id);
        }
        chapterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}