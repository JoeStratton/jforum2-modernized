package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.BanDto;
import net.jforum.dto.GroupDto;
import net.jforum.dto.ModerationLogDto;
import net.jforum.security.CurrentUser;
import net.jforum.service.ModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Admin API")
public class AdminController {

    private final ModerationService moderationService;

    @GetMapping("/moderation-logs")
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

    @GetMapping("/moderation-logs/user/{userId}")
    @Operation(
            summary = "Get moderation logs by user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<ModerationLogDto>> getModerationLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(moderationService.getModerationLogsByUser(userId, pageable));
    }

    @GetMapping("/moderation-logs/forum/{forumId}")
    @Operation(
            summary = "Get moderation logs by forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<ModerationLogDto>> getModerationLogsByForum(
            @PathVariable Long forumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(moderationService.getModerationLogsByForum(forumId, pageable));
    }

    @GetMapping("/bans")
    @Operation(
            summary = "Get active bans",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<BanDto>> getActiveBans() {
        return ResponseEntity.ok(moderationService.getActiveBans());
    }

    @GetMapping("/bans/user/{userId}")
    @Operation(
            summary = "Get ban by user ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<BanDto> getBanByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(moderationService.getBanByUserId(userId));
    }

    @PostMapping("/bans")
    @Operation(
            summary = "Ban a user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<BanDto> banUser(
            @Valid @RequestBody BanDto banDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        BanDto ban = moderationService.banUser(
                banDto.getUserId(),
                banDto.getReason(),
                banDto.getEndDate(),
                currentUser.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ban);
    }

    @DeleteMapping("/bans/user/{userId}")
    @Operation(
            summary = "Unban a user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> unbanUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        moderationService.unbanUser(userId, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("User unbanned successfully"));
    }

    @GetMapping("/groups")
    @Operation(
            summary = "Get all groups",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        return ResponseEntity.ok(moderationService.getAllGroups());
    }

    @PostMapping("/groups")
    @Operation(
            summary = "Create a group",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody GroupDto groupDto) {
        GroupDto group = moderationService.createGroup(
                groupDto.getName(),
                groupDto.getDescription(),
                groupDto.getParentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @PutMapping("/groups/{id}")
    @Operation(
            summary = "Update a group",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupDto> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody GroupDto groupDto) {
        GroupDto group = moderationService.updateGroup(
                id,
                groupDto.getName(),
                groupDto.getDescription(),
                groupDto.getParentId()
        );
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/groups/{id}")
    @Operation(
            summary = "Delete a group",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> deleteGroup(@PathVariable Long id) {
        moderationService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponseDto.success("Group deleted successfully"));
    }

    @PostMapping("/groups/{groupId}/users/{userId}")
    @Operation(
            summary = "Add user to group",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> addUserToGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        moderationService.addUserToGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponseDto.success("User added to group successfully"));
    }

    @DeleteMapping("/groups/{groupId}/users/{userId}")
    @Operation(
            summary = "Remove user from group",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> removeUserFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        moderationService.removeUserFromGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponseDto.success("User removed from group successfully"));
    }
}