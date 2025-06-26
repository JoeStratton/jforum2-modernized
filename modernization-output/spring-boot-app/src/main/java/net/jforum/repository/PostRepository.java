package net.jforum.repository;

import net.jforum.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByTopicIdOrderByCreatedAtAsc(Long topicId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.forum.moderated = true AND p.status = 'WAITING' ORDER BY p.createdAt ASC")
    Page<Post> findPostsAwaitingModeration(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.forum.id = :forumId AND p.forum.moderated = true AND p.status = 'WAITING' ORDER BY p.createdAt ASC")
    Page<Post> findPostsAwaitingModerationByForumId(@Param("forumId") Long forumId, Pageable pageable);
}