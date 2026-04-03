package senac.tsi.mangaVW.entities;

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
public class Manga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do mangá", example = "1")
    private Long id;


    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Título do manga", example = "Berserk")
    private String title;


    @NotBlank
    @Size(min=1, max=255)
    @Schema(description = "Sinopse do manga", example = "Um jovem deliquente chamado Sakuragi se atrai por uma garota que o convida para entrar em um time de basquete...")
    private String sinopsis;

    @NotNull
    @Schema(description = "Status de publicação do mangá", example = "FINALIZADO")
    @Enumerated(EnumType.STRING)
    private StatusPublication status;


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "manga_details_id", referencedColumnName = "id")
    @JsonIgnoreProperties("manga")
    @Schema(description = "Detalhes técnicos de publicação do mangá")
    private MangaDetails details;

    @NotNull(message = "O mangá precisa ter um autor vinculado")
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    @JsonIgnoreProperties("mangas")
    @Schema(description = "Autor responsável pela obra")
    private Author author;

    @OneToMany(mappedBy = "manga", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"manga", "pages"})
    @Schema(description = "Lista de capítulos lançados para este mangá")
    private List<Chapter> chapters = new ArrayList<>();


    @ManyToMany
    @JoinTable(
            name = "manga_genre", // Nome da tabela intermediária que será criada
            joinColumns = @JoinColumn(name = "manga_id"), // A chave estrangeira desta classe (Manga)
            inverseJoinColumns = @JoinColumn(name = "genre_id") // A chave estrangeira da outra classe (Genre)
    )
    @JsonIgnoreProperties("mangas")
    @Schema(description = "Lista de gêneros deste mangá")
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
