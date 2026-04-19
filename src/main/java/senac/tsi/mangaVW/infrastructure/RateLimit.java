package senac.tsi.mangaVW.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para definir limites de taxa (Rate Limit) dinâmicos por método/endpoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Número máximo de requisições permitidas (Padrão: 20)
     */
    int capacity() default 20;

    /**
     * Janela de tempo em minutos para o limite reiniciar (Padrão: 1)
     */
    int minutes() default 1;
}
