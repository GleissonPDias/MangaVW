package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.exceptions.MangaNotFoundException;
import senac.tsi.mangaVW.repositories.MangaRepository;
import senac.tsi.mangaVW.services.MangaDexService;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name="mangas", description = "Mangas route")
@RestController
public class MangaController {

    private final MangaRepository mangaRepository;
    private final PagedResourcesAssembler<Manga> pagedResourcesAssembler;
    private final MangaDexService mangaDexService;


    @Tag(name = "Search")
    @Operation(summary = "Search mangas by title", description = "Busca paginada de mangás contendo um título específico ignorando maiúsculas e minúsculas.")
    @GetMapping("/mangas/search")
    public ResponseEntity<PagedModel<EntityModel<Manga>>> searchMangasByTitle(
            @RequestParam String title,
            @ParameterObject Pageable pageable) {

        var mangas = mangaRepository.findByTitleContainingIgnoreCase(title, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(mangas));
    }

    @Autowired
    public MangaController(MangaRepository mangaRepository,
                           PagedResourcesAssembler<Manga> pagedResourcesAssembler,
                           MangaDexService mangaDexService) {
        this.mangaRepository = mangaRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.mangaDexService = mangaDexService;
    }

    @Operation(summary = "Sync mangas from MangaDex API", description = "Importa mangás da API pública do MangaDex para o banco de dados local.")
    @PostMapping("/mangas/sync")
    public ResponseEntity<String> syncFromMangaDex() {
        try {
            mangaDexService.syncMangasFromMangaDex();
            return ResponseEntity.ok("Sincronização com MangaDex concluída com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao sincronizar com MangaDex: " + e.getMessage());
        }
    }

    @Tag(name = "Get")
    @Operation(summary = "Get all mangas", description = """
            Get all mangas on the database, 
            even if the route returns one or less 
            itens the API still returns a list
            """)
    @GetMapping("/mangas")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Manga>>> getBooks(@ParameterObject Pageable pageable){
        var mangas = mangaRepository.findAll(pageable);

        PagedModel<EntityModel<Manga>> pagedModelBooks = pagedResourcesAssembler.toModel(mangas);

        return ResponseEntity.ok(pagedModelBooks);
    }

    @Tag(name = "Get Manga by id",
            description = "Get a single manga by id, or returns 404 not found")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the manga",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Manga.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Manga not found",
                    content = @Content) })
    @GetMapping("/mangas/{id}")
    public EntityModel<Manga> getBookById(
            @PathVariable(name = "id") long id){

        var manga = mangaRepository.findById(id)
                .orElseThrow(() -> new MangaNotFoundException(id));

        return EntityModel.of(manga,
                linkTo(methodOn(MangaController.class).getBookById(id)).withSelfRel(),
                linkTo(methodOn(MangaController.class).getBooks(Pageable.unpaged())).withRel("mangas"));
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manga created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Manga.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input provided") })
    @PostMapping("/mangas")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Manga> createBook(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Manga to create", required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Manga.class),
                    examples = @ExampleObject(value = "{ \"title\": \"New Manga\", \"author\": \"Author Name\" }")))
    @RequestBody Manga newManga){
        mangaRepository.save(newManga);
        return ResponseEntity.created(
                        URI.create("/mangas/"+ newManga.getId()))
                .body(newManga);

    }

    @PutMapping("/mangas/{id}")
    public ResponseEntity<Manga> updateBook(@PathVariable long id,
                                            @RequestBody Manga updatedManga){

        return mangaRepository.findById(id).map(
                manga -> {
                    manga.setTitle(updatedManga.getTitle());
                    manga.setAuthor(updatedManga.getAuthor());
                    manga.setSinopsis(updatedManga.getSinopsis());
                    manga.setStatus(updatedManga.getStatus());
                    return ResponseEntity.ok(mangaRepository.save(manga));
                }
        ).orElseGet(() -> {
            return ResponseEntity.created(URI.create("/mangas/"+
                    updatedManga.getId()))
                    .body(mangaRepository.save(updatedManga));
        });
    }

    @DeleteMapping("/mangas/{id}")
    public ResponseEntity deleteBook(@PathVariable long id){
        var manga = mangaRepository.findById(id).orElse(null);
        if(manga == null)
            return ResponseEntity.notFound().build();

        mangaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
