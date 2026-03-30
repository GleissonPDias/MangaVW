package senac.tsi.mangaVW.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.mangaVW.entities.MangaDetails;

public interface MangaDetailsRepository extends JpaRepository<MangaDetails, Long> {
    Page<MangaDetails> findByLicensedTrue(Pageable pageable);
}
