package senac.tsi.mangaVW.exceptions;

public class MangaNotFoundException extends RuntimeException {

    public MangaNotFoundException(Long id) {
        super("Could not find Manga " + id);
    }
}


