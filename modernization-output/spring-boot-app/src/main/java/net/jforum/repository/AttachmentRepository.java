package net.jforum.repository;

import net.jforum.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByPostId(Long postId);

    List<Attachment> findByUserId(Long userId);
}