package senac.tsi.mangaVW.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import senac.tsi.mangaVW.entities.Manga;
import senac.tsi.mangaVW.repositories.MangaRepository;

@Configuration
public class LoadDatabase {
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(MangaRepository repository){
        return args -> {
            log.info("Preloading" + repository.save(new
                    Manga("O senhor dos anéis", "J.R.R. Tolkien")));
            log.info("Preloading" + repository.save(new
                    Manga("Eu Robo", "Isaac Asimov")));
        };
    }
}
