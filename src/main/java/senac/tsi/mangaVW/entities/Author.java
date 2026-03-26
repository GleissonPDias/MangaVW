package senac.tsi.mangaVW.entities;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
public class Author {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do autor", example = "2")
    private long id;

    @NotNull
    @NotBlank
    @Size(min=1, max=50)
    @Schema(description = "Nome do autor", example = "Urasawa Naoki")
    private String name;

    @NotNull
    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Biografia do autor", example = "Naoki nasceu no...")
    private String biography;



    public Author() {}

    public Author(String name, String biography) {
        this.name = name;
        this.biography = biography;
    }

    public Author(long id, String name, String biography) {
        this.id = id;
        this.name = name;
        this.biography = biography;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
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
                ", name=" + name + '\'' +
                ", biography=" + biography + '\'' +
                '}';
    }
}
