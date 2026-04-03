package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Author>>> getAllAuthors(@ParameterObject Pageable pageable) {
        var authors = authorRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors));
    }

    @Operation(summary = "Search authors by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pesquisa realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "URL de pesquisa inválida"),
    })

    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Author>>> searchAuthorsByName(
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        var authors = authorRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors)); // Retorna 200 OK
    }

    @Operation(summary = "Get a single author by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autor encontrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "Autor não encontrado no banco de dados")
    })
    @GetMapping("/{id}")
    public ResponseEntity <EntityModel<Author>> getAuthorById(@PathVariable long id) {
        var author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id)); // Retorna 404 via Advice se não achar
        var entityModel = EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthorById(id)).withSelfRel(),
                linkTo(methodOn(AuthorController.class).getAllAuthors(Pageable.unpaged())).withRel("authors"));
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new author")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Autor criado com sucesso no banco de dados"),
            @ApiResponse(responseCode = "422", description = "Entidade não processável: Erro de validação dos campos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie os dados básicos do novo autor",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Masashi Kudemorto",
                                      "biography": "Autor de renome, criador da franquia Naruto."
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<Author> createAuthor(@Valid @RequestBody Author newAuthor) {
        Author savedAuthor = authorRepository.save(newAuthor);
        return ResponseEntity.created(URI.create("/authors/" + savedAuthor.getId())).body(savedAuthor);
    }

    @Operation(summary = "Update an existing author")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autor atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "JSON inválido fornecido na requisição"),
            @ApiResponse(responseCode = "404", description = "O autor que você está tentando atualizar não existe")
    })

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Atualize os dados do autor. Os mangás vinculados não são alterados por esta rota",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Masashi Kishimoto",
                                      "biography": "Criador de Naruto e Samurai 8."
                                    }
                                    """
                    )
            )
    )

    @PutMapping("/{id}")
    public ResponseEntity<Author> updateAuthor(@PathVariable long id, @Valid @RequestBody Author updatedAuthor) {
        return authorRepository.findById(id).map(author -> {
            author.setName(updatedAuthor.getName());
            author.setBiography(updatedAuthor.getBiography());
            return ResponseEntity.ok(authorRepository.save(author)); // Retorna 200 OK se atualizou
        }).orElseThrow(() -> new AuthorNotFoundException(id));


    }

    @Operation(summary = "Delete an author")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable long id) {
        if (!authorRepository.existsById(id)) {
            throw new AuthorNotFoundException(id); // Retorna 404 Not Found se tentar deletar algo que não existe
        }
        authorRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content se deletou com sucesso
    }
}