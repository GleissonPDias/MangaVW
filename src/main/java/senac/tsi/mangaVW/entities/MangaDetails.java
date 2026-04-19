package senac.tsi.mangaVW.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;

import java.util.Objects;

@Entity
public class MangaDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @Schema(description = "Unique details ID", example = "28")
    private long id;

    @Size(max = 20)
    @Schema(nullable = true, type = "string", description = "International Standard Book Number of the manga", example = "205-534-1-325")
    private String isbn;



    @Min(value = 1900)
    @Max(value = 2100)
    @Schema(description = "Year the manga was published", example = "2011")
    private int publicationYear;


    @Schema(description = "Is the work officially licensed in the country?", example = "true")
    private boolean licensed;

    @jakarta.persistence.OneToOne(mappedBy = "details")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"details"})
    @Schema(description = "Manga to which these details belong")
    private Manga manga;

    public MangaDetails() {}

    public MangaDetails(String isbn, int publicationYear, boolean licensed) {this.isbn = isbn;this.publicationYear = publicationYear; this.licensed = licensed;}

    public MangaDetails(long id, String isbn, int publicationYear, boolean licensed) {
        this.id = id;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.licensed = licensed;
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }
    public boolean isLicensed() { return licensed; }
    public void setLicensed(boolean licensed) { this.licensed = licensed; }

    public Manga getManga() { return manga; }
    public void setManga(Manga manga) { this.manga = manga; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MangaDetails details = (MangaDetails) o;
        return id == details.id && Objects.equals(isbn, details.isbn) && Objects.equals(publicationYear, details.publicationYear) && licensed == details.licensed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isbn, publicationYear, licensed);
    }

    @Override
    public String toString() {
        return "MangaDetails{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", publicationYear =" + publicationYear +
                ", licensed =" + licensed +
                '}';
    }
}
