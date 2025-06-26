package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.ModerationLogDto;
import net.jforum.dto.PostDto;
import net.jforum.security.CurrentUser;
import net.jforum.service.ModerationService;
import net.jforum.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/moderation")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "Moderation API")
public class ModerationController {

    private final PostService postService;
    private final ModerationService moderationService;

    @GetMapping("/queue")
    @Operation(
            summary = "Get posts awaiting moderation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<PostDto>> getPostsAwaitingModeration(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(postService.getPostsAwaitingModeration(pageable));
    }

    @GetMapping("/queue/forum/{forumId}")
    @Operation(
            summary = "Get posts awaiting moderation by forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<PostDto>> getPostsAwaitingModerationByForum(
            @PathVariable Long forumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(postService.getPostsAwaitingModerationByForum(forumId, pageable));
    }

    @PostMapping("/posts/{postId}/approve")
    @Operation(
            summary = "Approve a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> approvePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        postService.approvePost(postId, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Post approved successfully"));
    }

    @PostMapping("/posts/{postId}/reject")
    @Operation(
            summary = "Reject a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> rejectPost(
            @PathVariable Long postId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CurrentUser currentUser) {
        postService.rejectPost(postId, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Post rejected successfully"));
    }

    @GetMapping("/logs")
    @Operation(
            summary = "Get moderation logs",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<ModerationLogDto>> getModerationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(moderationService.getModerationLogs(pageable));
    }
}