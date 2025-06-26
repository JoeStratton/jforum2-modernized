package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.TopicDto;
import net.jforum.entity.Topic;
import net.jforum.security.CurrentUser;
import net.jforum.service.TopicService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Topic management API")
public class TopicController {

    private final TopicService topicService;

    @GetMapping("/forum/{forumId}")
    @Operation(summary = "Get topics by forum")
    public ResponseEntity<Page<TopicDto>> getTopicsByForum(
            @PathVariable Long forumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(topicService.getTopicsByForum(forumId, pageable));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent topics")
    public ResponseEntity<Page<TopicDto>> getRecentTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(topicService.getRecentTopics(pageable));
    }

    @GetMapping("/hot")
    @Operation(summary = "Get hot topics")
    public ResponseEntity<Page<TopicDto>> getHotTopics(
            @RequestParam(defaultValue = "100") int minViews,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(topicService.getHotTopics(minViews, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get topics by user")
    public ResponseEntity<Page<TopicDto>> getTopicsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(topicService.getTopicsByUser(userId, pageable));
    }

    @GetMapping("/watched")
    @Operation(
            summary = "Get watched topics",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<TopicDto>> getWatchedTopics(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(topicService.getWatchedTopics(currentUser.getId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get topic by ID")
    public ResponseEntity<TopicDto> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getTopicById(id));
    }

    @PostMapping("/forum/{forumId}")
    @Operation(
            summary = "Create a new topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<TopicDto> createTopic(
            @PathVariable Long forumId,
            @Valid @RequestBody TopicDto topicDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Topic.TopicType type = Topic.TopicType.valueOf(
                topicDto.getType() != null ? topicDto.getType() : "NORMAL");
        
        TopicDto createdTopic = topicService.createTopic(
                forumId,
                topicDto.getTitle(),
                topicDto.getPosts().get(0).getContent(),
                type,
                currentUser.getId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTopic);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<TopicDto> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicDto topicDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Topic.TopicType type = topicDto.getType() != null ? 
                Topic.TopicType.valueOf(topicDto.getType()) : null;
        
        return ResponseEntity.ok(topicService.updateTopic(
                id,
                topicDto.getTitle(),
                type,
                currentUser.getId()
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> deleteTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        topicService.deleteTopic(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Topic deleted successfully"));
    }

    @PostMapping("/{id}/lock")
    @Operation(
            summary = "Lock a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> lockTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        topicService.lockTopic(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Topic locked successfully"));
    }

    @PostMapping("/{id}/unlock")
    @Operation(
            summary = "Unlock a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> unlockTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        topicService.unlockTopic(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Topic unlocked successfully"));
    }

    @PostMapping("/{id}/move")
    @Operation(
            summary = "Move a topic to another forum",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> moveTopic(
            @PathVariable Long id,
            @RequestParam Long destinationForumId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        topicService.moveTopic(id, destinationForumId, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Topic moved successfully"));
    }

    @PostMapping("/{id}/subscriptions")
    @Operation(
            summary = "Subscribe to a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> subscribeTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        topicService.watchTopic(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Topic subscription added"));
    }

    @DeleteMapping("/{id}/subscriptions")
    @Operation(
            summary = "Unsubscribe from a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> unsubscribeTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        topicService.unwatchTopic(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Topic subscription removed"));
    }

    @GetMapping("/{id}/subscriptions/status")
    @Operation(
            summary = "Check if user is subscribed to a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Boolean>> isSubscribed(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        boolean isWatching = topicService.isWatching(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success(isWatching));
    }
}