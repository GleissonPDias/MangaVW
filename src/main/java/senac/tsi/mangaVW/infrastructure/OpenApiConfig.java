package senac.tsi.mangaVW.infrastructure;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

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
                version = "1",
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
                        
                        ### 🔐 Authentication (API Key)
                        This API operates statelessly. All operational endpoints require an **`X-API-Key`** header to be passed in every request. 
                        Public endpoints, such as the Swagger documentation or the `/api-keys` generation route, are exempt. Unauthenticated requests will receive a `401 Unauthorized` response.
                        
                        ### 🔄 Idempotency
                        To guarantee safe retries across distributed networks, all `POST` creation routes enforce strict idempotency logic. 
                        Clients must provide a unique **`Idempotency-Key`** in the request header. If the exact same payload is submitted with an existing key, the server returns the cached `201 Created` response. If the payload differs, it returns `409 Conflict`.
                        
                        ### 🔀 API Versioning
                        The API utilizes HTTP Headers for versioning (`X-API-Version`). 
                        By default, requests process as version 1. Certain endpoints (like Mangas) have a Version 2 implementation offering different payload structures. Pass `X-API-Version: 2` in your headers to access these experimental or full representations.

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
        ),
        security = @SecurityRequirement(name = "ApiKey")
)
@SecurityScheme(
        name = "ApiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "Authentication key strictly required for all protected API operations."
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
            
            // Clean up X-API-Version parameter to prevent "1.0.0" from showing in Swagger
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(param -> {
                    if ("X-API-Version".equals(param.getName()) && param.getSchema() != null) {
                        param.getSchema().setEnum(new java.util.ArrayList<>(java.util.List.of("1", "2")));
                        param.getSchema().setDefault("1");
                    }
                });
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