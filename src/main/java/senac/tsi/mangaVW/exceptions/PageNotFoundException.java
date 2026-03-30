package senac.tsi.mangaVW.exceptions;

public class PageNotFoundException extends RuntimeException {
    public PageNotFoundException(Long id) {
        super("Could not find Page " + id);
    }
}
