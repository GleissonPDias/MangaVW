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

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name="books", description = "Books route")
@RestController
public class MangaController {

    private final MangaRepository mangaRepository;
    private final PagedResourcesAssembler<Manga> pagedResourcesAssembler;


    @Autowired
    public MangaController(MangaRepository mangaRepository,
                           PagedResourcesAssembler<Manga> pagedResourcesAssembler) {
        this.mangaRepository = mangaRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Tag(name = "Get")
    @Operation(summary = "Get all books", description = """
            Get all books on the database, 
            even if the route returns one or less 
            itens the API still returns a list
            """)
    @GetMapping("/books")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Manga>>> getBooks(@ParameterObject Pageable pageable){
        var books = mangaRepository.findAll(pageable);

        PagedModel<EntityModel<Manga>> pagedModelBooks = pagedResourcesAssembler.toModel(books);

        return ResponseEntity.ok(pagedModelBooks);
    }

    @Tag(name = "Get Manga by id",
            description = "Get a single book by id, or returns 404 not found")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the book",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Manga.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Manga not found",
                    content = @Content) })
    @GetMapping("/books/{id}")
    public EntityModel<Manga> getBookById(
            @PathVariable(name = "id") long id){

        var book = mangaRepository.findById(id)
                .orElseThrow(() -> new MangaNotFoundException(id));

        return EntityModel.of(book,
                linkTo(methodOn(MangaController.class).getBookById(id)).withSelfRel(),
                linkTo(methodOn(MangaController.class).getBooks(Pageable.unpaged())).withRel("books"));
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manga created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Manga.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid input provided") })
    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Manga> createBook(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Manga to create", required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Manga.class),
                    examples = @ExampleObject(value = "{ \"title\": \"New Manga\", \"author\": \"Author Name\" }")))
    @RequestBody Manga newManga){
        mangaRepository.save(newManga);
        return ResponseEntity.created(
                        URI.create("/books/"+ newManga.getId()))
                .body(newManga);

    }

    @PutMapping("/books/{id}")
    public ResponseEntity<Manga> updateBook(@PathVariable long id,
                                            @RequestBody Manga updatedManga){

        return mangaRepository.findById(id).map(
                book -> {
                    book.setTitle(updatedManga.getTitle());
                    book.setAuthor(updatedManga.getAuthor());
                    return ResponseEntity.ok(mangaRepository.save(book));
                }
        ).orElseGet(() -> {
            return ResponseEntity.created(URI.create("/books/"+
                    updatedManga.getId()))
                    .body(mangaRepository.save(updatedManga));
        });
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity deleteBook(@PathVariable long id){
        var book = mangaRepository.findById(id).orElse(null);
        if(book == null)
            return ResponseEntity.notFound().build();

        mangaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
