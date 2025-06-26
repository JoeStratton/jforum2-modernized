package net.jforum.repository;

import net.jforum.entity.PrivateMessageRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivateMessageRecipientRepository extends JpaRepository<PrivateMessageRecipient, Long> {

    Optional<PrivateMessageRecipient> findByMessageIdAndUserId(Long messageId, Long userId);
}