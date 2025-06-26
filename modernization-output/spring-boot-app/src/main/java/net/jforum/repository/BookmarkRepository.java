package net.jforum.repository;

import net.jforum.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Page<Bookmark> findByUserIdAndType(Long userId, Bookmark.BookmarkType type, Pageable pageable);

    Optional<Bookmark> findByUserIdAndTypeAndElementId(Long userId, Bookmark.BookmarkType type, Long elementId);
}