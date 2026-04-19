package senac.tsi.mangaVW.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @Schema(description = "Unique chapter ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Chapter number", example = "10.5", type = "number", format = "double", implementation = Double.class, nullable = true)
    private Double chapterNumber;

    @NotBlank
    @Schema(description = "Chapter language", example = "PT-BR")
    @Size(min = 2, max = 20, message = "The language must have at least 2 characters")
    private String language;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("chapter")
    @Schema(description = "List of pages that make up this chapter")
    private List<Page> pages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"author", "details", "chapters"})
    @JoinColumn(name = "manga_id", nullable = false)
    @Schema(description = "Manga to which this chapter belongs", accessMode = Schema.AccessMode.READ_WRITE)
    private Manga manga;

    public Chapter() {}

    public Chapter(Double chapterNumber, String language) {
        this.chapterNumber = chapterNumber;
        this.language = language;
    }

    public Chapter(Long id, Double chapterNumber, String language) {
        this.id = id;
        this.chapterNumber = chapterNumber;
        this.language = language;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Double chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<Page> getPages() { return pages; }
    public void setPages(List<Page> pages) { this.pages = pages; }

    public Manga getManga() { return manga; }
    public void setManga(Manga manga) { this.manga = manga; }

    // ✅ equals e hashCode corrigidos
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chapter)) return false;
        Chapter chapter = (Chapter) o;
        return Objects.equals(id, chapter.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id=" + id +
                ", chapterNumber=" + chapterNumber +
                ", language='" + language + '\'' +
                '}';
    }
}