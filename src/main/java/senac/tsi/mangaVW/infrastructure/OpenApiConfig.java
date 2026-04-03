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
                title = "Your API Title",
                version = "1.0.0",
                description = "A description of your API.",
                contact = @Contact(
                        name = "Your Name",
                        email = "your.email@example.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org" // URL to the MIT license
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