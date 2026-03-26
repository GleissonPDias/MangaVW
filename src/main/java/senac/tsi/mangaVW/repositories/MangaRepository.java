package senac.tsi.mangaVW.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.mangaVW.entities.Manga;

@Repository
public interface MangaRepository extends JpaRepository<Manga, Long> { }
