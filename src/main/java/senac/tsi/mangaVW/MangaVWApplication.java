package senac.tsi.mangaVW;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "MangaVW API",
                version = "1.0.0",
                description = "API RESTful para gerenciamento de um acervo de mangás. " +
                        "Projeto desenvolvido como requisito para o curso de TSI no Senac, " +
                        "aplicando modelagem de dados, paginação e boas práticas de arquitetura REST.",
                contact = @Contact(
                        name = "Gleisson",
                        email = "gleisson.gpd10@gmail.com",
                        url = "https://github.com/GleissonPDias"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
public class MangaVWApplication {

	public static void main(String[] args) {
		SpringApplication.run(MangaVWApplication.class, args);
	}

}
