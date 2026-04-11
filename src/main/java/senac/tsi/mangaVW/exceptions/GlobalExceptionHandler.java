package senac.tsi.mangaVW.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 🛠️ Cria a estrutura JSON usando o novo Record tipado!
    private ApiErrorResponse createErrorBody(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path.replace("uri=", "") // Deixa o caminho mais limpo no JSON
        );
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        String msg = "Erro de integridade: Os dados enviados violam restrições do banco (ex: ID duplicado ou campo obrigatório faltando).";
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, msg, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ Captura erro de tipo nos parâmetros da URL (ex: id=a em vez de id=1)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String error = String.format("O parâmetro '%s' deveria ser do tipo %s, mas recebeu '%s'",
                ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue());
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, error, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ Captura o erro do nosso PaginationInterceptor (page=a)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ Erros do @Valid direto no Controller (ex: campo em branco no JSON)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        String msg = "Validation Error: Os dados enviados estão incorretos ou incompletos. Verifique o JSON da requisição.";
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, msg, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ Erros de validação escondidos no Hibernate
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiErrorResponse> handleTransactionSystemException(TransactionSystemException ex, WebRequest request) {
        String msg = "Transaction Error: Falha ao processar os dados. Verifique limites de banco ou objetos vazios.";
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, msg, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ JSON malformado (mandar string em boolean)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String msg = "JSON Error: O corpo da requisição está malformado ou contém tipos de dados inválidos.";
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, msg, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ Erros de conversão e constraints
    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleConversion(ConversionFailedException ex, WebRequest request) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String msg = "Validation Error: Regras de validação violadas. Detalhes: " + ex.getMessage();
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, msg, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ CAPTURA GERAL DE NOT FOUNDS (404)
    @ExceptionHandler({
            MangaNotFoundException.class, AuthorNotFoundException.class,
            GenreNotFoundException.class, ChapterNotFoundException.class,
            PageNotFoundException.class, MangaDetailsNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false)), HttpStatus.NOT_FOUND);
    }

    // 🛡️ ESCUDO FINAL (Para não dar erro 500 feio)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        System.err.println("🚨 Erro não tratado capturado: " + ex.getClass().getName() + " - " + ex.getMessage());
        String msg = "Bad Request: O servidor não conseguiu processar a requisição. Verifique os campos e parâmetros.";
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, msg, request.getDescription(false)), HttpStatus.BAD_REQUEST);
    }

    // 🛡️ Método não suportado (ex: método QUERY do Schemathesis)
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {

        String msg = "Method Not Allowed: O método " + ex.getMethod() + " não é suportado neste endpoint.";

        // 🚨 O SEGREDO: Criamos o Header "Allow" dizendo quais métodos são aceitos (GET, POST, etc.)
        var headers = new org.springframework.http.HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }

        return new ResponseEntity<>(
                createErrorBody(HttpStatus.METHOD_NOT_ALLOWED, msg, request.getDescription(false)),
                headers, // Passamos o header para a resposta
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }
}