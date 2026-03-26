package senac.tsi.mangaVW.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;


@Entity
public class Manga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do mangá", example = "1")
    private long id;

    @NotNull
    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Título do manga", example = "Berserk")
    private String title;

    @NotNull
    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Sinopse do manga", example = "Um jovem deliquente chamado Sakuragi se atrai por uma garota que o convida para entrar em um time de basquete...")
    private String sinopsis;

    @NotNull
    @Schema(description = "Status de publicação do mangá", example = "FINALIZADO")
    @Enumerated(EnumType.STRING)
    StatusPublication status;



    public Manga() {
    }

    public Manga(String title, String sinopsis, StatusPublication status) {
        this.title = title;
        this.sinopsis = sinopsis;
        this.status = status;
    }

    public Manga(long id, String title, String sinopsis, StatusPublication status) {
        this.id = id;
        this.title = title;
        this.sinopsis = sinopsis;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public StatusPublication getStatus() {
        return status;
    }
    public void setStatus(StatusPublication status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Manga manga = (Manga) o;
        return id == manga.id && Objects.equals(title, manga.title) && Objects.equals(sinopsis, manga.sinopsis) && status == manga.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, sinopsis, status);
    }

    @Override
    public String toString() {
        return "Manga{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", sinopsis='" + sinopsis + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
