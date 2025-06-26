package net.jforum.repository;

import net.jforum.entity.Forum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {

    List<Forum> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);

    @Query("SELECT f FROM Forum f JOIN f.moderators m WHERE m.id = :userId")
    List<Forum> findByModeratorId(@Param("userId") Long userId);
}