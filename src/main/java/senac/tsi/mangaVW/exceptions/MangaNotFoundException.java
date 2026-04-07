package senac.tsi.mangaVW.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MangaNotFoundException extends RuntimeException {

    public MangaNotFoundException(Long id) {
        super("Could not find Manga " + id);
    }
}


