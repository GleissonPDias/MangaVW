package senac.tsi.mangaVW;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class MangaVWApplication {

	public static void main(String[] args) {
		// 🌍 Força o idioma da aplicação para Inglês, garantindo que as validações automáticas do Java saiam em inglês
		Locale.setDefault(Locale.ENGLISH);
		SpringApplication.run(MangaVWApplication.class, args);
	}

}
