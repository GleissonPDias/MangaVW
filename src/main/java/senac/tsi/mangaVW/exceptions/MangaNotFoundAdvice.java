package senac.tsi.mangaVW.exceptions;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MangaNotFoundAdvice {

    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleConversion(ConversionFailedException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(MangaNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String mangaNotFoundHandler(MangaNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(AuthorNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String authorNotFoundHandler(AuthorNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(GenreNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String genreNotFoundHandler(GenreNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ChapterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String chapterNotFoundHandler(ChapterNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(PageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String pageNotFoundHandler(PageNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(MangaDetailsNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String mangaDetailsNotFoundHandler(MangaDetailsNotFoundException ex) {
        return ex.getMessage();
    }


}