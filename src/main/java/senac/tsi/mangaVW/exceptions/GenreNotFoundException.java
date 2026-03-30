package senac.tsi.mangaVW.exceptions;

public class GenreNotFoundException extends RuntimeException {
    public GenreNotFoundException(Long id) {
        super("Could not find Genre " + id);
    }
}
