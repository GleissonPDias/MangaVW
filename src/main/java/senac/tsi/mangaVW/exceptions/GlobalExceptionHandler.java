package senac.tsi.mangaVW.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

import java.util.LinkedHashMap;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 🛡️ 1. Captura erros barrados pelo @Valid direto no Controller (ex: campo em branco no JSON)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return "Validation Error: Os dados enviados estão incorretos ou incompletos. Verifique o JSON da requisição.";
    }

    // 🛡️ 2. Captura erros de validação que o Hibernate escondeu dentro de uma falha de transação
    @ExceptionHandler(TransactionSystemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleTransactionSystemException(TransactionSystemException ex) {
        return "Transaction Error: Falha ao processar os dados. Verifique se você enviou objetos vazios ({}) ou dados que ferem as regras de limite do banco.";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleAllExceptions(Exception ex) {
        // É importante logar no console do Java para VOCÊ saber o que quebrou
        System.err.println("🚨 Erro não tratado capturado: " + ex.getClass().getName() + " - " + ex.getMessage());
        return "Bad Request: A requisição enviou dados que o servidor não conseguiu processar. Verifique os campos e parâmetros.";
    }

    // 🛡️ 4. Captura JSON malformado ou tipos trocados (ex: mandar string em campo boolean)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return "JSON Error: O corpo da requisição está malformado ou contém tipos de dados inválidos.";
    }

    // 🛡️ 5. Captura erros de argumentos ilegais (comum no 'sort=null' ou filtros zoados)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleIllegalArgument(IllegalArgumentException ex) {
        return "Argument Error: Um ou mais parâmetros de busca são inválidos.";
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    String handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return "Method Not Allowed: Este endpoint não suporta o método " + ex.getMethod();
    }


    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleConversion(ConversionFailedException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
    // Se a mensagem contiver algo sobre chave duplicada, mantemos o 409
    if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate")) {
        return new ResponseEntity<>(
            createErrorBody(HttpStatus.CONFLICT, "Dados duplicados detectados.", request.getDescription(false)), 
            HttpStatus.CONFLICT
        );
    }
    
    // Para todos os outros erros de banco durante um GET ou POST, tratamos como Bad Request
    return new ResponseEntity<>(
        createErrorBody(HttpStatus.BAD_REQUEST, "Erro de integridade nos dados enviados.", request.getDescription(false)), 
        HttpStatus.BAD_REQUEST
    );
}

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleConstraintViolation(ConstraintViolationException ex) {
        return "Validation Error: Um ou mais campos enviados ferem as regras de validação (ex: enviou um objeto vazio que exigia dados). Detalhes: " + ex.getMessage();
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