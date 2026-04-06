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

@Tag(name = "Authors", description = "Endpoints for managing manga authors and their biographical data")
@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorRepository authorRepository;
    private final PagedResourcesAssembler<Author> pagedResourcesAssembler;

    @Autowired
    public AuthorController(AuthorRepository authorRepository,
                            PagedResourcesAssembler<Author> pagedResourcesAssembler) {
        this.authorRepository = authorRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all authors", description = "Retrieves a paginated list of all registered authors in the database. Includes embedded HATEOAS links for navigation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping

    public ResponseEntity<PagedModel<EntityModel<Author>>> getAllAuthors(@ParameterObject Pageable pageable) {
        var authors = authorRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors));
    }

    @Operation(summary = "Search authors by name", description = "Performs a case-insensitive search for authors matching the provided name keyword. Returns a paginated response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pesquisa realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "URL de pesquisa inválida"),
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Author>>> searchAuthorsByName(
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        var authors = authorRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(authors));
    }

    @Operation(summary = "Get author by ID", description = "Retrieves the detailed profile of a specific author using their unique identifier. Includes self, update, and delete HATEOAS links.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autor encontrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "Autor não encontrado no banco de dados")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Author>> getAuthorById(@PathVariable long id) {
        var author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));

        // Utilizando o método auxiliar para gerar os links
        return ResponseEntity.ok(toEntityModel(author));
    }

    @Operation(summary = "Create a new author", description = "Registers a new author in the system. Requires a valid name and biography. Returns the created resource location.")
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
                                      "name": "Masashi Kishimoto",
                                      "biography": "Autor de renome, criador da franquia Naruto."
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<EntityModel<Author>> createAuthor(@Valid @RequestBody Author newAuthor) {
        Author savedAuthor = authorRepository.save(newAuthor);

        // Retorna o 201 Created junto com o EntityModel contendo os links
        return ResponseEntity
                .created(URI.create("/authors/" + savedAuthor.getId()))
                .body(toEntityModel(savedAuthor));
    }

    @Operation(summary = "Update an author", description = "Updates the biographical information of an existing author. Linked mangas are preserved and not affected by this operation.")
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
    public ResponseEntity<EntityModel<Author>> updateAuthor(@PathVariable long id, @Valid @RequestBody Author updatedAuthor) {
        return authorRepository.findById(id).map(author -> {
            author.setName(updatedAuthor.getName());
            author.setBiography(updatedAuthor.getBiography());

            Author savedAuthor = authorRepository.save(author);

            // Retorna o 200 OK junto com o EntityModel contendo os links atualizados
            return ResponseEntity.ok(toEntityModel(savedAuthor));

        }).orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @Operation(summary = "Delete an author", description = "Permanently removes an author from the database. Due to database constraints, associated entities may be affected based on cascade rules.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable long id) {
        if (!authorRepository.existsById(id)) {
            throw new AuthorNotFoundException(id);
        }
        authorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- MÉTODO AUXILIAR PARA HATEOAS ---
    // Centraliza a criação dos links, evitando repetição de código no GET, POST e PUT
    private EntityModel<Author> toEntityModel(Author author) {
        return EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthorById(author.getId())).withSelfRel(),
                linkTo(methodOn(AuthorController.class).updateAuthor(author.getId(), null)).withRel("update"),
                linkTo(methodOn(AuthorController.class).deleteAuthor(author.getId())).withRel("delete"),
                linkTo(methodOn(AuthorController.class).getAllAuthors(Pageable.unpaged())).withRel("authors"));
    }
}