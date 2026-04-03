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

// IMPORTANTE: Importando a SUA entidade Page
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
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Page>>> getAllPages(@ParameterObject Pageable pageable) {
        var pages = pageRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages));
    }

    @Operation(summary = "Search pages by image URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pesquisa realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "URL de pesquisa inválida"),
    })

    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Page>>> searchPagesByUrl(
            @RequestParam String imageUrl, @ParameterObject Pageable pageable) {
        var pages = pageRepository.findByImageUrlContainingIgnoreCase(imageUrl, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(pages));
    }

    @Operation(summary = "Get a single page by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página encontrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "Página não encontrada no banco de dados")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Page>> getPageById(@PathVariable long id) {
        var page = pageRepository.findById(id).orElseThrow(() -> new PageNotFoundException(id));
        var entityModel =  EntityModel.of(page,
                linkTo(methodOn(PageController.class).getPageById(id)).withSelfRel(),
                linkTo(methodOn(PageController.class).getAllPages(Pageable.unpaged())).withRel("pages"));
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Página criada com sucesso no banco de dados"),
            @ApiResponse(responseCode = "400", description = "JSON mal formatado ou faltando o ID do Capítulo"),
            @ApiResponse(responseCode = "404", description = "O Capítulo informado não existe no banco de dados"),
            @ApiResponse(responseCode = "409", description = "Conflito: A página já existe neste capítulo"),
            @ApiResponse(responseCode = "422", description = "Entidade não processável: Erro de validação dos campos")
    })

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie os dados da página e apenas o ID do capítulo desejado",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "pageNumber": 28,
                                      "imageUrl": "https://mangadex/berserk/cap10/16",
                                      "chapter": {
                                        "id": 1
                                      }
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<Page> createPage(@Valid @RequestBody Page newPage) {
        if(newPage.getChapter() == null || newPage.getChapter().getId() == null){
            return ResponseEntity.badRequest().build();
        }

        Chapter chapter = chapterRepository.findById(newPage.getChapter().getId())
                .orElseThrow(() -> new ChapterNotFoundException(newPage.getChapter().getId()));
        newPage.setChapter(chapter);

        Page savedPage = pageRepository.save(newPage);

        return ResponseEntity
                .created(URI.create("/pages/" + savedPage.getId()))
                .body(savedPage);
    }





    @Operation(summary = "Update an existing page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "JSON inválido fornecido na requisição"),
            @ApiResponse(responseCode = "404", description = "A página que você está tentando atualizar não existe")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie apenas os campos da página que serão atualizados (o vínculo com o capítulo não é alterado por aqui).",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "pageNumber": 30,
                                      "imageUrl": "https://mangadex/berserk/cap10/16-atualizada.jpg"
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<Page> updatePage(@PathVariable long id, @Valid @RequestBody Page updatedPage) {
        return pageRepository.findById(id).map(page -> {
            page.setPageNumber(updatedPage.getPageNumber());
            page.setImageUrl(updatedPage.getImageUrl());
            return ResponseEntity.ok(pageRepository.save(page));
        }).orElseThrow(() -> new PageNotFoundException(id)) ;
    }

    @Operation(summary = "Delete a page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deletado com sucesso!"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "A página informada não existe no banco de dados"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable long id) {
        if (!pageRepository.existsById(id))  {
            throw new PageNotFoundException(id);
        }
        pageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}