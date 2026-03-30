package senac.tsi.mangaVW.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Chapter {

    //id (long): IDENTITY.

            //chapterNumber (String ou double): O número do capítulo (ex: "1", "12", "15.5"). Usar String ou double previne problemas com capítulos fracionados. Não pode ser vazio/nulo.

           // title (String): Título do capítulo (ex: "O Retorno do Herói"). Pode ser opcional (muitos mangás não dão nomes aos capítulos, só números).

   // language (String): O idioma do capítulo (ex: "pt-br", "en"). Muito importante se a ideia é puxar dados de outras APIs, pois costuma haver várias traduções.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID unico do capitulo", example = "1")
    private long id;


    @Schema(description = "Numero do capitulo", example = "10.5")
    private double chapterNumber;

    @NotBlank
    @NotNull
    @Schema(description = "Idioma do capitulo", example = "PT-BR")
    @Size(min = 1, max = 20)
    private String language;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL)
    @Schema(description = "Lista de páginas que compõem este capítulo")
    private List<Page> pages = new ArrayList<>();


    @NotNull(message = "O capítulo precisa pertencer a um mangá")
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "manga_id", nullable = false)
    @Schema(description = "Mangá ao qual este capítulo pertence")
    private Manga manga;

    public Manga getManga() {
        return manga;
    }
    public void setManga(Manga manga) {this.manga = manga;}

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {this.pages = pages;}

    public Chapter() {}

    public Chapter(double chapterNumber, String language)
    {this.chapterNumber = chapterNumber;this.language = language;}

    public Chapter(long id, double chapterNumber, String language)
    {this.id = id; this.chapterNumber = chapterNumber; this.language = language;}

    public long getId() {return id;}
    public void setId(long id) {    this.id = id;}

    public double getChapterNumber() {return chapterNumber;}
    public void setChapterNumber(double chapterNumber) {this.chapterNumber = chapterNumber;}

    public String getLanguage() {return language;}
    public void setLanguage(String language) {this.language = language;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chapter chapter = (Chapter) o;
        return id ==  chapter.id && Objects.equals(chapterNumber, chapter.chapterNumber) && Objects.equals(language, chapter.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chapterNumber, language);
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id=" + id +
                ", chapterNumber='" + chapterNumber + '\'' +
                ", language='" + language + '\'' +
                '}';
    }




}
