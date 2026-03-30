package senac.tsi.mangaVW.exceptions;

public class ChapterNotFoundException extends RuntimeException {
    public ChapterNotFoundException(Long id) {
        super("Could not find Chapter " + id);
    }
}
