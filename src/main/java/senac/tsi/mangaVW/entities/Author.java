package senac.tsi.mangaVW.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Author {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do autor", example = "2")
    private Long id;


    @NotBlank
    @Size(min=1, max=50)
    @Schema(description = "Nome do autor", example = "Urasawa Naoki")
    private String name;


    @NotBlank
    @Column(columnDefinition = "TEXT")
    @Schema(description = "Biografia do autor", example = "Naoki nasceu no...")
    private String biography;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("author")
    @Schema(description = "Lista de mangás escritos por este autor")
    private List<Manga> mangas = new ArrayList<>();

    public List<Manga> getMangas() {
        return mangas;
    }
    public void setMangas(List<Manga> mangas) {this.mangas = mangas;}

    public Author() {}

    public Author(String name, String biography) {
        this.name = name;
        this.biography = biography;
    }

    public Author(Long id, String name, String biography) {
        this.id = id;
        this.name = name;
        this.biography = biography;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getBiography() {return biography;}
    public void setBiography(String biography) {this.biography = biography;}

    @Override
   public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return id == author.id && Objects.equals(name, author.name) && Objects.equals(biography, author.biography);
    }
    @Override
    public int hashCode() {return Objects.hash(id, name, biography);}

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", biography='" + biography + '\'' +
                '}';
    }
}
