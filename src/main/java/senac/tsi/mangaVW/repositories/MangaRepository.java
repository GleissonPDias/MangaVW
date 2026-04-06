package senac.tsi.mangaVW.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.mangaVW.entities.Manga;

import java.util.Optional;


@Repository
public interface MangaRepository extends JpaRepository<Manga, Long> {

    Optional<Manga> findByDetailsId(Long detailsId);

    Page<Manga> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
