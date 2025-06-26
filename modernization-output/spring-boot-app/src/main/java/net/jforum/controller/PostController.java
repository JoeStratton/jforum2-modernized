package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.PostDto;
import net.jforum.security.CurrentUser;
import net.jforum.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post management API")
public class PostController {

    private final PostService postService;

    @GetMapping("/topic/{topicId}")
    @Operation(summary = "Get posts by topic")
    public ResponseEntity<Page<PostDto>> getPostsByTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(postService.getPostsByTopic(topicId, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user")
    public ResponseEntity<Page<PostDto>> getPostsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(postService.getPostsByUser(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<PostDto> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @PostMapping("/topic/{topicId}")
    @Operation(
            summary = "Create a new post in a topic",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostDto> createPost(
            @PathVariable Long topicId,
            @Valid @RequestBody PostDto postDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        PostDto createdPost = postService.createPostWithAttachments(
                topicId,
                postDto.getContent(),
                currentUser.getId(),
                postDto.getAttachments() != null ? 
                        postDto.getAttachments().stream().map(a -> a.getId()).toList() : 
                        null
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostDto> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostDto postDto,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(postService.updatePost(
                id,
                postDto.getContent(),
                currentUser.getId()
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        postService.deletePost(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Post deleted successfully"));
    }

    @GetMapping("/{id}/quote")
    @Operation(
            summary = "Get a post as a quote",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<String>> getQuotedPost(@PathVariable Long id) {
        String quotedContent = postService.getQuotedPost(id);
        return ResponseEntity.ok(ApiResponseDto.success(quotedContent));
    }

    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponseDto<Void>> approvePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        postService.approvePost(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Post approved successfully"));
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject a post",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponseDto<Void>> rejectPost(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        postService.rejectPost(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Post rejected successfully"));
    }
}