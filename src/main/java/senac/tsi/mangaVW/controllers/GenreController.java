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
import senac.tsi.mangaVW.entities.Genre;
import senac.tsi.mangaVW.exceptions.GenreNotFoundException;
import senac.tsi.mangaVW.repositories.GenreRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name="genres", description = "Genres route")
@RestController
@RequestMapping("/genres")
public class GenreController {

    private final GenreRepository genreRepository;
    private final PagedResourcesAssembler<Genre> pagedResourcesAssembler;

    @Autowired
    public GenreController(GenreRepository genreRepository, PagedResourcesAssembler<Genre> pagedResourcesAssembler) {
        this.genreRepository = genreRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all genres paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getAllGenres(@ParameterObject Pageable pageable) {
        var genres = genreRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genres));
    }

    @Operation(summary = "Search genres by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pesquisa realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "URL de pesquisa inválida"),
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> searchGenresByName(
            @RequestParam String name, @ParameterObject Pageable pageable) {
        var genres = genreRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genres));
    }

    @Operation(summary = "Get a single genre by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gênero encontrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido"),
            @ApiResponse(responseCode = "404", description = "Gênero não encontrado no banco de dados")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Genre>> getGenreById(@PathVariable long id) {
        var genre = genreRepository.findById(id).orElseThrow(() -> new GenreNotFoundException(id));
        var entityModel = EntityModel.of(genre,
                linkTo(methodOn(GenreController.class).getGenreById(id)).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres(Pageable.unpaged())).withRel("genres"));
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new genre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Gênero criado com sucesso no banco de dados"),
            @ApiResponse(responseCode = "400", description = "JSON mal formatado"),
            @ApiResponse(responseCode = "422", description = "Entidade não processável: Erro de validação dos campos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Envie somente o nome do gênero",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Romance"
                                    }
                                    """
                    )
            )
    )

    @PostMapping
    public ResponseEntity<Genre> createGenre(@Valid @RequestBody Genre newGenre) {
        Genre savedGenre = genreRepository.save(newGenre);
        return ResponseEntity.created(URI.create("/genres/" + savedGenre.getId())).body(savedGenre);
    }

    @Operation(summary = "Update an existing genre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "gênero atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "JSON inválido fornecido na requisição"),
            @ApiResponse(responseCode = "404", description = "O gênero que você está tentando atualizar não existe")
    })

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Atualize o nome do gênero. Os mangás vinculados não são alterados por esta rota",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = """
                                    {
                                      "name": "Terror"
                                    }
                                    """
                    )
            )
    )

    @PutMapping("/{id}")
    public ResponseEntity<Genre> updateGenre(@PathVariable long id, @Valid @RequestBody Genre updatedGenre) {
        return genreRepository.findById(id).map(genre -> {
            genre.setName(updatedGenre.getName());
            return ResponseEntity.ok(genreRepository.save(genre));
        }).orElseThrow(() -> new GenreNotFoundException(id));
    }

    @Operation(summary = "Delete a genre")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable long id) {
        if (!genreRepository.existsById(id)){
            throw new GenreNotFoundException(id);
        }
        genreRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}