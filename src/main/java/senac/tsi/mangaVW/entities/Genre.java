package senac.tsi.mangaVW.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @Schema(description = "Unique genre ID", example = "2")
    private Long id;


    @NotBlank
    @Column(unique = true)
    @Size(min=2, max=50, message = "The genre name must be between 2 and 50 characters")
    @Schema(description = "Genre name", example = "Fantasy")
    private String name;


    @ManyToMany(mappedBy = "genres")
    @JsonIgnoreProperties("genres")
    @Schema(description = "List of mangas that belong to this genre")
    private List<Manga> mangas = new ArrayList<>();

    public List<Manga> getMangas() {
        return mangas;
    }

    public void setMangas(List<Manga> mangas) {this.mangas = mangas;}

    public Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

    public Genre(Long id, String name){
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return id == genre.id && Objects.equals(name, genre.name);
    }

    @Override
    public int hashCode() {return Objects.hash(id, name);}

    @Override
    public String toString() {
        return "Genre{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }


}
