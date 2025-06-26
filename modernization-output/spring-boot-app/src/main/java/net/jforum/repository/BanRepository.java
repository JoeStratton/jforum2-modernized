package net.jforum.repository;

import net.jforum.entity.Ban;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BanRepository extends JpaRepository<Ban, Long> {

    Optional<Ban> findByUserId(Long userId);

    Optional<Ban> findByIpAddress(String ipAddress);

    Optional<Ban> findByEmail(String email);

    @Query("SELECT b FROM Ban b WHERE b.endDate IS NULL OR b.endDate > :now")
    List<Ban> findActiveBans(@Param("now") LocalDateTime now);
}