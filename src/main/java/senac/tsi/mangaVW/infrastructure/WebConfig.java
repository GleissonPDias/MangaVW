package senac.tsi.mangaVW.infrastructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Conecta o Interceptador na Malha Web do Spring Boot
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // Permite qualquer origem (pode ser restrito depois)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Retry-After")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Intercepta todas as rotas da API (**)
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
    }

    @Override
    public void configureApiVersioning(org.springframework.web.servlet.config.annotation.ApiVersionConfigurer configurer) {
        configurer.addSupportedVersions("1", "2")
                  .setDefaultVersion("1")
                  .useRequestHeader("X-API-Version");
    }
}
