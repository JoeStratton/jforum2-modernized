package net.jforum.repository;

import net.jforum.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    Page<Topic> findByForumIdOrderByTypeDescCreatedAtDesc(Long forumId, Pageable pageable);

    @Query("SELECT t FROM Topic t ORDER BY t.lastPost.createdAt DESC")
    Page<Topic> findRecentTopics(Pageable pageable);

    @Query("SELECT t FROM Topic t WHERE t.viewCount > :viewCount ORDER BY t.viewCount DESC")
    Page<Topic> findHotTopics(@Param("viewCount") int viewCount, Pageable pageable);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId")
    Page<Topic> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Topic t JOIN t.watchers w WHERE w.id = :userId")
    Page<Topic> findWatchedTopics(@Param("userId") Long userId, Pageable pageable);
}