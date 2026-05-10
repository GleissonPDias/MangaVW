package senac.tsi.mangaVW.infrastructure;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptador invisível que lê as anotações @RateLimit de cada Controller
 * antes mesmos deles processarem a requisição!
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Gavetário central para guardar todos os baldes da API
    // Chave no formato: "ID do Cliente - Nome do Endpoint"
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Ignora requisições de PREFLIGHT (CORS) para evitar bloqueios indevidos de Rate Limit
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        // Verifica se a requisição está sendo enviada para um de nossos métodos Java do Controller
        if (handler instanceof HandlerMethod handlerMethod) {

            // Extrai a nossa anotação especial @RateLimit se ela existir no método
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null) {

                // 1. Descobre a identidade do crimi... ops, do cliente! (Tratando Proxies)
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }

                // 2. Fabrica uma Chave única do tipo "192.168.0.1-getAllMangas"
                String endpointName = handlerMethod.getMethod().getName();
                String bucketKey = ip + "-" + endpointName;

                // 3. Resgata do cofre o Balde atual do usuário ou constrói um sob medida baseado na anotação!
                Bucket userBucket = buckets.computeIfAbsent(bucketKey, key -> createNewBucket(rateLimit));

                // 4. Regra da Catraca: Usando Probe para extrair limites e injetar cabeçalhos
                ConsumptionProbe probe = userBucket.tryConsumeAndReturnRemaining(1);
                
                if (probe.isConsumed()) {
                    response.addHeader("X-RateLimit-Limit", String.valueOf(rateLimit.capacity()));
                    response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
                } else {
                    long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                    
                    response.addHeader("X-RateLimit-Limit", String.valueOf(rateLimit.capacity()));
                    response.addHeader("X-RateLimit-Remaining", "0");
                    response.addHeader("Retry-After", String.valueOf(waitForRefill));
                    
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
                    response.setContentType("application/json"); // <- Swagger agora fica feliz!
                    response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded for this endpoint.\"}");
                    return false; // Retorna FALSE cortando a viagem para o Controller
                }
            }
        }
        
        return true; // Se passar da catraca ou se não tiver RateLimit configurado, libera a passagem!
    }

    // Fabricante Oficial de Baldes personalizados
    private Bucket createNewBucket(RateLimit config) {
        Bandwidth limit = Bandwidth.classic(
                config.capacity(), 
                Refill.intervally(config.capacity(), Duration.ofMinutes(config.minutes()))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
