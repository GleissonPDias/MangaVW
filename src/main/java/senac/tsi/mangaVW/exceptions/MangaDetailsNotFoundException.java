package senac.tsi.mangaVW.exceptions;

public class MangaDetailsNotFoundException extends RuntimeException {
    public MangaDetailsNotFoundException(Long id) {
        super("Could not find MangaDetails " + id);
    }
}
