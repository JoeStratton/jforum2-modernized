package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.ForumDto;
import net.jforum.entity.User;
import net.jforum.security.CurrentUser;
import net.jforum.service.ForumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/forums")
@RequiredArgsConstructor
@Tag(name = "Forums", description = "Forum management API")
public class ForumController {

    private final ForumService forumService;

    @GetMapping
    @Operation(summary = "Get all forums")
    public ResponseEntity<List<ForumDto>> getAllForums() {
        return ResponseEntity.ok(forumService.getAllForums());
    }

    @GetMapping("/by-category/{categoryId}")
    @Operation(summary = "Get forums by category")
    public ResponseEntity<List<ForumDto>> getForumsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(forumService.getForumsByCategory(categoryId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get forum by ID")
    public ResponseEntity<ForumDto> getForumById(@PathVariable Long id) {
        return ResponseEntity.ok(forumService.getForumById(id));
    }

    @PostMapping
    @Operation(
            summary = "Create a new forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ForumDto> createForum(@Valid @RequestBody ForumDto forumDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(forumService.createForum(forumDto));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ForumDto> updateForum(
            @PathVariable Long id,
            @Valid @RequestBody ForumDto forumDto) {
        return ResponseEntity.ok(forumService.updateForum(id, forumDto));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteForum(@PathVariable Long id) {
        forumService.deleteForum(id);
        return ResponseEntity.ok(ApiResponseDto.success("Forum deleted successfully"));
    }

    @PostMapping("/{id}/moderators/{userId}")
    @Operation(
            summary = "Add a moderator to a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> addModerator(
            @PathVariable Long id,
            @PathVariable Long userId) {
        forumService.addModerator(id, userId);
        return ResponseEntity.ok(ApiResponseDto.success("Moderator added successfully"));
    }

    @DeleteMapping("/{id}/moderators/{userId}")
    @Operation(
            summary = "Remove a moderator from a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> removeModerator(
            @PathVariable Long id,
            @PathVariable Long userId) {
        forumService.removeModerator(id, userId);
        return ResponseEntity.ok(ApiResponseDto.success("Moderator removed successfully"));
    }

    @PostMapping("/{id}/subscriptions")
    @Operation(
            summary = "Subscribe to a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> subscribeForum(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        forumService.watchForum(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Forum subscription added"));
    }

    @DeleteMapping("/{id}/subscriptions")
    @Operation(
            summary = "Unsubscribe from a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> unsubscribeForum(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        forumService.unwatchForum(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Forum subscription removed"));
    }

    @GetMapping("/{id}/subscriptions/status")
    @Operation(
            summary = "Check if user is subscribed to a forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Boolean>> isSubscribed(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        boolean isWatching = forumService.isWatching(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success(isWatching));
    }
}