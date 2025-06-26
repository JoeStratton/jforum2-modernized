package net.jforum.repository;

import net.jforum.entity.SearchIndex;
import net.jforum.entity.SearchWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Long> {

    @Query("SELECT si FROM SearchIndex si WHERE si.word.id IN :wordIds ORDER BY si.relevance DESC")
    Page<SearchIndex> findByWordIds(@Param("wordIds") List<Long> wordIds, Pageable pageable);

    @Query("SELECT si FROM SearchIndex si WHERE si.word.id IN :wordIds AND si.forum.id = :forumId ORDER BY si.relevance DESC")
    Page<SearchIndex> findByWordIdsAndForumId(@Param("wordIds") List<Long> wordIds, @Param("forumId") Long forumId, Pageable pageable);

    @Query("SELECT si FROM SearchIndex si WHERE si.post.user.id = :userId ORDER BY si.post.createdAt DESC")
    Page<SearchIndex> findByUserId(@Param("userId") Long userId, Pageable pageable);
}