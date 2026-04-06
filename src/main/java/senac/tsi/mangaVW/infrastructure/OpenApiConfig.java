package senac.tsi.mangaVW.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

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
public class OpenApiConfig {

    @Bean
    public OperationCustomizer customizerRemoveNotFoundFromLists() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            String methodName = handlerMethod.getMethod().getName();

            // Adicionamos "create" à lista. Agora qualquer método que comece
            // com "create" terá o 404 removido automaticamente do Swagger.
            if (methodName.startsWith("getAll") ||
                    methodName.startsWith("search") ||
                    methodName.startsWith("create")) {

                operation.getResponses().remove("404");
            }

            return operation;
        };
    }
}