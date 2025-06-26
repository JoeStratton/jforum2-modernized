package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.PrivateMessageDto;
import net.jforum.dto.UserSummaryDto;
import net.jforum.entity.PrivateMessage;
import net.jforum.entity.PrivateMessageRecipient;
import net.jforum.entity.User;
import net.jforum.exception.BadRequestException;
import net.jforum.exception.ForbiddenException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.PrivateMessageRecipientRepository;
import net.jforum.repository.PrivateMessageRepository;
import net.jforum.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private final PrivateMessageRepository messageRepository;
    private final PrivateMessageRecipientRepository recipientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<PrivateMessageDto> getMessagesByFolder(Long userId, PrivateMessageRecipient.FolderType folder, Pageable pageable) {
        return messageRepository.findMessagesByUserIdAndFolder(userId, folder, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PrivateMessageDto> getSentMessages(Long userId, Pageable pageable) {
        return messageRepository.findSentMessages(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PrivateMessageDto getMessageById(Long id, Long userId) {
        PrivateMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));
        
        // Check if user is sender or recipient
        if (!message.getFromUser().getId().equals(userId) && 
                message.getRecipients().stream().noneMatch(r -> r.getUser().getId().equals(userId))) {
            throw new ForbiddenException("You don't have permission to view this message");
        }
        
        // If user is a recipient, mark as read
        message.getRecipients().stream()
                .filter(r -> r.getUser().getId().equals(userId) && !r.isRead())
                .findFirst()
                .ifPresent(r -> {
                    r.setRead(true);
                    recipientRepository.save(r);
                });
        
        return mapToDto(message);
    }

    @Transactional
    public PrivateMessageDto sendMessage(String subject, String content, Long fromUserId, List<Long> toUserIds, Long parentMessageId) {
        if (toUserIds == null || toUserIds.isEmpty()) {
            throw new BadRequestException("At least one recipient is required");
        }
        
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", fromUserId));
        
        // Get parent message if specified
        PrivateMessage parentMessage = null;
        PrivateMessage.MessageType type = PrivateMessage.MessageType.STANDARD;
        
        if (parentMessageId != null) {
            parentMessage = messageRepository.findById(parentMessageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message", "id", parentMessageId));
            
            // Check if user is sender or recipient of parent message
            if (!parentMessage.getFromUser().getId().equals(fromUserId) && 
                    parentMessage.getRecipients().stream().noneMatch(r -> r.getUser().getId().equals(fromUserId))) {
                throw new ForbiddenException("You don't have permission to reply to this message");
            }
            
            type = PrivateMessage.MessageType.REPLY;
        }
        
        // Create message
        PrivateMessage message = PrivateMessage.builder()
                .subject(subject)
                .content(content)
                .type(type)
                .fromUser(fromUser)
                .parentMessage(parentMessage)
                .recipients(new HashSet<>())
                .build();
        
        PrivateMessage savedMessage = messageRepository.save(message);
        
        // Add recipients
        Set<PrivateMessageRecipient> recipients = new HashSet<>();
        for (Long toUserId : toUserIds) {
            User toUser = userRepository.findById(toUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", toUserId));
            
            PrivateMessageRecipient recipient = PrivateMessageRecipient.builder()
                    .message(savedMessage)
                    .user(toUser)
                    .read(false)
                    .flagged(false)
                    .folder(PrivateMessageRecipient.FolderType.INBOX)
                    .build();
            
            recipients.add(recipientRepository.save(recipient));
        }
        
        savedMessage.setRecipients(recipients);
        
        // TODO: Send notifications to recipients
        
        return mapToDto(savedMessage);
    }

    @Transactional
    public PrivateMessageDto forwardMessage(Long messageId, String content, Long fromUserId, List<Long> toUserIds) {
        PrivateMessage originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));
        
        // Check if user is sender or recipient
        if (!originalMessage.getFromUser().getId().equals(fromUserId) && 
                originalMessage.getRecipients().stream().noneMatch(r -> r.getUser().getId().equals(fromUserId))) {
            throw new ForbiddenException("You don't have permission to forward this message");
        }
        
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", fromUserId));
        
        // Create forwarded message content
        String forwardedContent = content + "\n\n----- Forwarded Message -----\n" +
                "From: " + originalMessage.getFromUser().getUsername() + "\n" +
                "Subject: " + originalMessage.getSubject() + "\n\n" +
                originalMessage.getContent();
        
        // Create new message
        PrivateMessage message = PrivateMessage.builder()
                .subject("Fwd: " + originalMessage.getSubject())
                .content(forwardedContent)
                .type(PrivateMessage.MessageType.FORWARD)
                .fromUser(fromUser)
                .parentMessage(originalMessage)
                .recipients(new HashSet<>())
                .build();
        
        PrivateMessage savedMessage = messageRepository.save(message);
        
        // Add recipients
        Set<PrivateMessageRecipient> recipients = new HashSet<>();
        for (Long toUserId : toUserIds) {
            User toUser = userRepository.findById(toUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", toUserId));
            
            PrivateMessageRecipient recipient = PrivateMessageRecipient.builder()
                    .message(savedMessage)
                    .user(toUser)
                    .read(false)
                    .flagged(false)
                    .folder(PrivateMessageRecipient.FolderType.INBOX)
                    .build();
            
            recipients.add(recipientRepository.save(recipient));
        }
        
        savedMessage.setRecipients(recipients);
        
        // TODO: Send notifications to recipients
        
        return mapToDto(savedMessage);
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        PrivateMessageRecipient recipient = recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found or you are not a recipient"));
        
        recipient.setRead(true);
        recipientRepository.save(recipient);
    }

    @Transactional
    public void markAsUnread(Long messageId, Long userId) {
        PrivateMessageRecipient recipient = recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found or you are not a recipient"));
        
        recipient.setRead(false);
        recipientRepository.save(recipient);
    }

    @Transactional
    public void moveToFolder(Long messageId, Long userId, PrivateMessageRecipient.FolderType folder) {
        PrivateMessageRecipient recipient = recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found or you are not a recipient"));
        
        recipient.setFolder(folder);
        recipientRepository.save(recipient);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId, boolean permanent) {
        PrivateMessageRecipient recipient = recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found or you are not a recipient"));
        
        if (permanent) {
            recipientRepository.delete(recipient);
        } else {
            recipient.setFolder(PrivateMessageRecipient.FolderType.TRASH);
            recipientRepository.save(recipient);
        }
    }

    // Helper method to map PrivateMessage entity to PrivateMessageDto
    private PrivateMessageDto mapToDto(PrivateMessage message) {
        return PrivateMessageDto.builder()
                .id(message.getId())
                .subject(message.getSubject())
                .content(message.getContent())
                .type(message.getType().name())
                .fromUser(message.getFromUser() != null ? 
                        UserSummaryDto.builder()
                                .id(message.getFromUser().getId())
                                .username(message.getFromUser().getUsername())
                                .avatarUrl(message.getFromUser().getAvatarUrl())
                                .build() : 
                        null)
                .toUserIds(message.getRecipients().stream()
                        .map(r -> r.getUser().getId())
                        .collect(Collectors.toList()))
                .parentMessageId(message.getParentMessage() != null ? message.getParentMessage().getId() : null)
                .build();
    }
}