package senac.tsi.mangaVW.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.mangaVW.entities.Page;


public interface PageRepository extends JpaRepository<Page, Long> {
    org.springframework.data.domain.Page<Page> findByImageUrlContainingIgnoreCase(
            String imageUrl,
            org.springframework.data.domain.Pageable pageable
    );
}
