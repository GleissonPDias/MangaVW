package senac.tsi.mangaVW.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Entity
public class Manga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @Schema(description = "Unique identifier of the manga", example = "1")
    private Long id;


    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Title of the manga", example = "Berserk")
    private String title;


    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Synopsis of the manga", example = "A young delinquent named Sakuragi...")
    private String sinopsis;

    @NotNull
    @Schema(description = "Publication status of the manga", example = "FINALIZADO")
    @Enumerated(EnumType.STRING)
    private StatusPublication status;


    @Valid
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "manga_details_id", referencedColumnName = "id")
    @JsonIgnoreProperties("manga")
    @Schema(description = "Technical publication details of the manga")
    private MangaDetails details;

    @NotNull(message = "The manga must have a linked author")
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    @JsonIgnoreProperties("mangas")
    @Schema(description = "Author responsible for the work")
    private Author author;

    @OneToMany(mappedBy = "manga", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"manga", "pages"})
    @Schema(description = "List of chapters released for this manga")
    private List<Chapter> chapters = new ArrayList<>();


    @ManyToMany
    @JoinTable(
            name = "manga_genre", // Nome da tabela intermediária que será criada
            joinColumns = @JoinColumn(name = "manga_id"), // A chave estrangeira desta classe (Manga)
            inverseJoinColumns = @JoinColumn(name = "genre_id") // A chave estrangeira da outra classe (Genre)
    )
    @JsonIgnoreProperties("mangas")
    @Schema(description = "List of genres for this manga")
    private List<Genre> genres = new ArrayList<>();

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {this.genres = genres;}

    public List<Chapter> getChapters() {
        return chapters;
    }
    public void setChapters(List<Chapter> chapters) {this.chapters = chapters;}

    public Author getAuthor() { return author; }

    public void setAuthor(Author author) { this.author = author; }

    public MangaDetails getDetails() {
        return details;
    }
    public void setDetails(MangaDetails details) {
        this.details = details;
    }

    public Manga() {
    }

    public Manga(String title, String sinopsis, StatusPublication status) {
        this.title = title;
        this.sinopsis = sinopsis;
        this.status = status;
    }

    public Manga(Long id, String title, String sinopsis, StatusPublication status) {
        this.id = id;
        this.title = title;
        this.sinopsis = sinopsis;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
