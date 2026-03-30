package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import senac.tsi.mangaVW.entities.Author;
import senac.tsi.mangaVW.exceptions.AuthorNotFoundException;
import senac.tsi.mangaVW.repositories.AuthorRepository;

import jakarta.validation.Valid;
import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="authors", description = "Authors route")
@RestController
@RequestMapping("/authors") // Isso evita repetir "/authors" em todos os métodos!
public class AuthorController {

    private final AuthorRepository authorRepository;
    private final PagedResourcesAssembler<Author> pagedResourcesAssembler;

    @Autowired
    public AuthorController(AuthorRepository authorRepository,
                            PagedResourcesAssembler<Author> pagedResourcesAssembler) {
        this.authorRepository = authorRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all authors paginated")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Author>>> getAllAuthors(@ParameterObject Pageable pageable) {
        var authors = authorRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors)); // Retorna 200 OK
    }

    @Operation(summary = "Search authors by name")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Author>>> searchAuthorsByName(
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        var authors = authorRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors)); // Retorna 200 OK
    }

    @Operation(summary = "Get a single author by id")
    @GetMapping("/{id}")
    public EntityModel<Author> getAuthorById(@PathVariable long id) {
        var author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id)); // Retorna 404 via Advice se não achar

        return EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthorById(id)).withSelfRel(),
                linkTo(methodOn(AuthorController.class).getAllAuthors(Pageable.unpaged())).withRel("authors")); // Retorna 200 OK
    }

    @Operation(summary = "Create a new author")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Força o retorno 201 Created
    public ResponseEntity<Author> createAuthor(@Valid @RequestBody Author newAuthor) {
        // O @Valid garante que se o nome vier vazio, o Spring barra e devolve 400 Bad Request automático!
        Author savedAuthor = authorRepository.save(newAuthor);
        return ResponseEntity.created(URI.create("/authors/" + savedAuthor.getId())).body(savedAuthor);
    }

    @Operation(summary = "Update an existing author")
    @PutMapping("/{id}")
    public ResponseEntity<Author> updateAuthor(@PathVariable long id, @Valid @RequestBody Author updatedAuthor) {
        return authorRepository.findById(id).map(author -> {
            author.setName(updatedAuthor.getName());
            author.setBiography(updatedAuthor.getBiography());
            return ResponseEntity.ok(authorRepository.save(author)); // Retorna 200 OK se atualizou
        }).orElseGet(() -> {
            updatedAuthor.setId(id);
            return ResponseEntity.created(URI.create("/authors/" + id)).body(authorRepository.save(updatedAuthor)); // Retorna 201 Created se não existia e foi criado
        });
    }

    @Operation(summary = "Delete an author")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable long id) {
        if (!authorRepository.existsById(id)) {
            return ResponseEntity.notFound().build(); // Retorna 404 Not Found se tentar deletar algo que não existe
        }
        authorRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content se deletou com sucesso
    }
}