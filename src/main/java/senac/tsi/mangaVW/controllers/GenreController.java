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
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getAllGenres(@ParameterObject Pageable pageable) {
        var genres = genreRepository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genres));
    }

    @Operation(summary = "Search genres by name")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> searchGenresByName(
            @RequestParam String name, @ParameterObject Pageable pageable) {
        var genres = genreRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genres));
    }

    @Operation(summary = "Get a single genre by id")
    @GetMapping("/{id}")
    public EntityModel<Genre> getGenreById(@PathVariable long id) {
        var genre = genreRepository.findById(id).orElseThrow(() -> new GenreNotFoundException(id));
        return EntityModel.of(genre,
                linkTo(methodOn(GenreController.class).getGenreById(id)).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres(Pageable.unpaged())).withRel("genres"));
    }

    @Operation(summary = "Create a new genre")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Genre> createGenre(@Valid @RequestBody Genre newGenre) {
        Genre savedGenre = genreRepository.save(newGenre);
        return ResponseEntity.created(URI.create("/genres/" + savedGenre.getId())).body(savedGenre);
    }

    @Operation(summary = "Update an existing genre")
    @PutMapping("/{id}")
    public ResponseEntity<Genre> updateGenre(@PathVariable long id, @Valid @RequestBody Genre updatedGenre) {
        return genreRepository.findById(id).map(genre -> {
            genre.setName(updatedGenre.getName());
            return ResponseEntity.ok(genreRepository.save(genre));
        }).orElseGet(() -> {
            updatedGenre.setId(id);
            return ResponseEntity.created(URI.create("/genres/" + id)).body(genreRepository.save(updatedGenre));
        });
    }

    @Operation(summary = "Delete a genre")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable long id) {
        if (!genreRepository.existsById(id)) return ResponseEntity.notFound().build();
        genreRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}