package senac.tsi.mangaVW.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PaginationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String page = request.getParameter("page");
        String size = request.getParameter("size");

        // Se o parâmetro existir, mas não for composto apenas por números (\d+), estouramos o erro
        if (page != null && !page.matches("\\d+")) {
            throw new IllegalArgumentException("O parâmetro 'page' deve conter apenas números. Recebido: " + page);
        }
        if (size != null && !size.matches("\\d+")) {
            throw new IllegalArgumentException("O parâmetro 'size' deve conter apenas números. Recebido: " + size);
        }

        // Se estiver tudo certo, a requisição segue normalmente
        return true;
    }
}