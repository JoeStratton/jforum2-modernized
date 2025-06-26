package net.jforum.repository;

import net.jforum.entity.ModerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {

    Page<ModerationLog> findByUserId(Long userId, Pageable pageable);

    Page<ModerationLog> findByForumId(Long forumId, Pageable pageable);

    Page<ModerationLog> findByTopicId(Long topicId, Pageable pageable);

    Page<ModerationLog> findByType(ModerationLog.LogType type, Pageable pageable);
}