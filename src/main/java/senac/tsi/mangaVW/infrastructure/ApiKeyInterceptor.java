package senac.tsi.mangaVW.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import senac.tsi.mangaVW.repositories.ApiKeyRepository;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyRepository apiKeyRepository;

    @Autowired
    public ApiKeyInterceptor(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Ignora requisições de PREFLIGHT (CORS) para evitar bloqueios indevidos exigindo API Key
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            
            RequireApiKey requireApiKey = handlerMethod.getMethodAnnotation(RequireApiKey.class);
            
            // Também verifica se a classe (Controller) inteira tem a anotação
            if (requireApiKey == null) {
                requireApiKey = handlerMethod.getBeanType().getAnnotation(RequireApiKey.class);
            }

            if (requireApiKey != null) {
                String apiKeyHeader = request.getHeader("X-API-Key");

                if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Missing X-API-Key header\"}");
                    return false;
                }

                boolean isValidAndActive = apiKeyRepository.findByKeyAndActiveTrue(apiKeyHeader).isPresent();

                if (!isValidAndActive) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or inactive API Key\"}");
                    return false;
                }
            }
        }
        return true;
    }
}
