package senac.tsi.mangaVW.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;


import java.util.Objects;

@Entity
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da pagina", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    private long id;


    @Schema(description = "Número da página", example = "28")
    private int pageNumber;

    @Schema(description = "URL da imagem", example = "https://mangadex/berserk/cap10/16")
    @NotBlank
    private String imageUrl;


    @ManyToOne
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonIgnoreProperties({"chapterNumber", "language", "pages", "manga"})
    @JoinColumn(name = "chapter_id", nullable = false)
    @Schema(description = "Capítulo ao qual esta página pertence")
    private Chapter chapter;

    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }


    public Page() {}

    public Page(int pageNumber, String imageUrl) {this.pageNumber = pageNumber;this.imageUrl = imageUrl;}

    public Page(long id, int pageNumber, String imageUrl) {this.id = id;this.pageNumber = pageNumber;this.imageUrl = imageUrl;}

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}

    public int getPageNumber() {return pageNumber;}
    public void setPageNumber(int pageNumber) {this.pageNumber = pageNumber;}

    public String getImageUrl() {return imageUrl;}
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return id == page.id && pageNumber == page.pageNumber && Objects.equals(imageUrl, page.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pageNumber, imageUrl);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", pageNumber='" + pageNumber + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }

}
