package senac.tsi.mangaVW.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MangaVW API",
                version = "1.0.0",
                description = """
                        **RESTful API for Comprehensive Manga Catalog Management.**
                        
                        This project was developed as a requirement for the TSI course at Senac, applying advanced data modeling and strict REST architectural best practices.
                        
                        ### 🚀 Key Features
                        * **Richardson Maturity Model Level 3:** Full HATEOAS implementation for resource discoverability.
                        * **Pagination:** Optimized data retrieval using Spring Data Pageable.
                        * **External Integration:** Automated background synchronization with the official MangaDex API.
                        * **Relational Integrity:** Complex One-To-One, One-To-Many, and Many-To-Many mappings managed by Hibernate.
                        
                        ### 🛠️ Tech Stack
                        **Java 17** | **Spring Boot 3** | **Spring Data JPA** | **H2 Database** | **Springdoc OpenAPI**
                        
                        ---
                        
                        ### ⚖️ Rate Limits (Bucket4j)
                        The MangaVW API implements strict, IP-based rate limiting to prevent abuse and ensure high availability. The application uses a hard-interval algorithm (tokens replenish strictly at the end of the time window).
                        
                        * **`GET` (List Operations)**: 20 requests per minute
                        
                        * **`GET` (Lookup by ID)**: 40 requests per minute

                        * **`GET` (Search Operations)**: 2 requests per minute

                        * **`POST` / `PUT` (Write Operations)**: 10 requests per 5 minutes

                        * **`DELETE`**: 5 requests per 10 minutes

                        * **`/sync` (External fetch)**: 1 request per 30 minutes
                        
                        Submitting excessive requests to the API server will result in an **HTTP 429 Too Many Requests** status code.
                        
                        It is not acceptable to ignore HTTP 429 responses. Continuing to overload the API after receiving a 429 status will result in your IP remaining blackholed until your time window expires.
                        """,
                contact = @Contact(
                        name = "Gleisson",
                        url = "https://github.com/GleissonPDias",
                        email = "gleisson.gpd10@gmail.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
// 🛡️ Garante suporte a paginação moderna e DTOs
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class OpenApiConfig implements WebMvcConfigurer { // ⬅️ Adicionado "implements"

    /**
     * 🛠️ CONFIGURAÇÃO DE PAGINAÇÃO RÍGIDA
     * Este método força o Spring a lançar uma exceção quando recebe parâmetros inválidos
     * (como letras em campos de números), permitindo que seu GlobalExceptionHandler retorne 400.
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();

        // Desativa o comportamento de "ignorar erro e usar padrão"
        resolver.setFallbackPageable(PageRequest.of(0, 20));

        // Adiciona no início da lista de resolvers para ter prioridade
        resolvers.add(0, resolver);
    }

    @Bean
    public OperationCustomizer customizerRemoveNotFoundFromLists() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            String methodName = handlerMethod.getMethod().getName();

            if (methodName.startsWith("getAll") ||
                    methodName.startsWith("search")) {

                operation.getResponses().remove("404");
            }

            return operation;
        };
    }
    @Autowired
    private PaginationInterceptor paginationInterceptor;

    // 🛡️ Registra a nossa barreira de proteção
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(paginationInterceptor);
    }
}