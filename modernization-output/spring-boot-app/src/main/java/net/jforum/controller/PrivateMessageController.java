package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.PrivateMessageDto;
import net.jforum.entity.PrivateMessageRecipient;
import net.jforum.security.CurrentUser;
import net.jforum.service.PrivateMessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Private Messages", description = "Private messaging API")
public class PrivateMessageController {

    private final PrivateMessageService messageService;

    @GetMapping
    @Operation(
            summary = "Get messages by folder",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<PrivateMessageDto>> getMessagesByFolder(
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        PrivateMessageRecipient.FolderType folderType = PrivateMessageRecipient.FolderType.valueOf(folder.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return ResponseEntity.ok(messageService.getMessagesByFolder(currentUser.getId(), folderType, pageable));
    }

    @GetMapping("/sent")
    @Operation(
            summary = "Get sent messages",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<PrivateMessageDto>> getSentMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(messageService.getSentMessages(currentUser.getId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get message by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PrivateMessageDto> getMessageById(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        return ResponseEntity.ok(messageService.getMessageById(id, currentUser.getId()));
    }

    @PostMapping
    @Operation(
            summary = "Send a new private message",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PrivateMessageDto> sendMessage(
            @Valid @RequestBody PrivateMessageDto messageDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        PrivateMessageDto sentMessage = messageService.sendMessage(
                messageDto.getSubject(),
                messageDto.getContent(),
                currentUser.getId(),
                messageDto.getToUserIds(),
                messageDto.getParentMessageId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(sentMessage);
    }

    @PostMapping("/{id}/reply")
    @Operation(
            summary = "Reply to a private message",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PrivateMessageDto> replyToMessage(
            @PathVariable Long id,
            @Valid @RequestBody PrivateMessageDto messageDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        // For a reply, we use the same recipients as the original message
        // and set the parent message ID to the message being replied to
        PrivateMessageDto originalMessage = messageService.getMessageById(id, currentUser.getId());
        
        PrivateMessageDto sentMessage = messageService.sendMessage(
                "Re: " + originalMessage.getSubject(),
                messageDto.getContent(),
                currentUser.getId(),
                originalMessage.getFromUser().getId() != currentUser.getId() ?
                        List.of(originalMessage.getFromUser().getId()) :
                        originalMessage.getToUserIds(),
                id
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(sentMessage);
    }

    @PostMapping("/{id}/forward")
    @Operation(
            summary = "Forward a private message",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PrivateMessageDto> forwardMessage(
            @PathVariable Long id,
            @Valid @RequestBody PrivateMessageDto messageDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        PrivateMessageDto forwardedMessage = messageService.forwardMessage(
                id,
                messageDto.getContent(),
                currentUser.getId(),
                messageDto.getToUserIds()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(forwardedMessage);
    }

    @PostMapping("/{id}/read")
    @Operation(
            summary = "Mark message as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        messageService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Message marked as read"));
    }

    @PostMapping("/{id}/unread")
    @Operation(
            summary = "Mark message as unread",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> markAsUnread(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        messageService.markAsUnread(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Message marked as unread"));
    }

    @PostMapping("/{id}/move")
    @Operation(
            summary = "Move message to folder",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> moveToFolder(
            @PathVariable Long id,
            @RequestParam String folder,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        PrivateMessageRecipient.FolderType folderType = PrivateMessageRecipient.FolderType.valueOf(folder.toUpperCase());
        messageService.moveToFolder(id, currentUser.getId(), folderType);
        return ResponseEntity.ok(ApiResponseDto.success("Message moved to " + folder));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete message",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> deleteMessage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean permanent,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        messageService.deleteMessage(id, currentUser.getId(), permanent);
        return ResponseEntity.ok(ApiResponseDto.success("Message deleted"));
    }
}