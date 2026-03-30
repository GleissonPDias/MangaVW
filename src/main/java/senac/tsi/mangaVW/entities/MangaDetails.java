package senac.tsi.mangaVW.entities;

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
    @Schema(description = "ID únicos dos detalhes", example = "28")
    private long id;

    @NotNull
    @NotBlank
    @Size(max = 20)
    @Schema(description = "Código de barras internacional do manga", example = "205-534-1-325")
    private String isbn;



    @Min(1900)
    @Max(2100)
    @Schema(description = "Ano em que o mangá começou a ser publicado", example = "2011")
    private int publicationYear;


    @Schema(description = "A obra tem licenciamento oficial no pais?", example = "true")
    private boolean licensed;

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
    public boolean getLicensed() { return licensed; }
    public void setLicensed(boolean licensed) { this.licensed = licensed; }

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
                ", publicationYear ='" + publicationYear + '\'' +
                ", licensed ='" + licensed + '\'' +
                '}';
    }
}
