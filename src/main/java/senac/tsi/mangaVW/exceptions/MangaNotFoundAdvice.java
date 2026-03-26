package senac.tsi.mangaVW.exceptions;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MangaNotFoundAdvice {

    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleConversion(MangaNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(MangaNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String bookNotFoundHandler(MangaNotFoundException ex) {
        return ex.getMessage();
    }
}
