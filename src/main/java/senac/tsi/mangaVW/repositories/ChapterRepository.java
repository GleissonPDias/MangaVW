package senac.tsi.mangaVW.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.mangaVW.entities.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Page<Chapter> findByLanguageIgnoreCase(String language, Pageable pageable);
}
