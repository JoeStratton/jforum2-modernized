package net.jforum.repository;

import net.jforum.entity.PrivateMessage;
import net.jforum.entity.PrivateMessageRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    @Query("SELECT pm FROM PrivateMessage pm WHERE pm.fromUser.id = :userId")
    Page<PrivateMessage> findSentMessages(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT pm FROM PrivateMessage pm JOIN pm.recipients r WHERE r.user.id = :userId AND r.folder = :folder")
    Page<PrivateMessage> findMessagesByUserIdAndFolder(@Param("userId") Long userId, @Param("folder") PrivateMessageRecipient.FolderType folder, Pageable pageable);
}