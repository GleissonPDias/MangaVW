package senac.tsi.mangaVW.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.mangaVW.entities.Author;
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.entities.StatusPublication; // Importe o seu Enum
import senac.tsi.mangaVW.repositories.AuthorRepository; // Importe o Repository do Autor
import senac.tsi.mangaVW.repositories.MangaRepository;

@Configuration
public class LoadDatabase {
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(MangaRepository mangaRepository, AuthorRepository authorRepository){
        return args -> {

            // 1. Primeiro criamos e salvamos o Autor no banco de dados
            Author autorTeste = new Author("Kentaro Miura", "Famoso autor de obras de fantasia sombria.");
            authorRepository.save(autorTeste);

            // 2. Criamos o primeiro Mangá (usando o Enum correto)
            Manga manga1 = new Manga("Berserk", "A história de Guts...", StatusPublication.FINALIZADO);
            manga1.setAuthor(autorTeste); // Aqui nós fazemos a ligação obrigatória!

            // 3. Criamos o segundo Mangá
            Manga manga2 = new Manga("Gigantomakhia", "Uma história curta...", StatusPublication.FINALIZADO);
            manga2.setAuthor(autorTeste); // Ligando ao mesmo autor

            // 4. Salvamos os mangás no banco de dados
            log.info("Preloading " + mangaRepository.save(manga1));
            log.info("Preloading " + mangaRepository.save(manga2));
        };
    }
}