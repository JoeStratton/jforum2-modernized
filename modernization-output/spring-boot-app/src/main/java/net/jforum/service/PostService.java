package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.AttachmentDto;
import net.jforum.dto.PostDto;
import net.jforum.dto.UserSummaryDto;
import net.jforum.entity.Forum;
import net.jforum.entity.Post;
import net.jforum.entity.Topic;
import net.jforum.entity.User;
import net.jforum.exception.BadRequestException;
import net.jforum.exception.ForbiddenException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.PostRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final TopicRepository topicRepository;
    private final ForumRepository forumRepository;
    private final UserRepository userRepository;
    private final ForumService forumService;
    private final AttachmentService attachmentService;

    @Transactional(readOnly = true)
    public Page<PostDto> getPostsByTopic(Long topicId, Pageable pageable) {
        // Verify topic exists
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic", "id", topicId);
        }
        
        return postRepository.findByTopicIdOrderByCreatedAtAsc(topicId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getPostsByUser(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        return postRepository.findByUserId(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getPostsAwaitingModeration(Pageable pageable) {
        return postRepository.findPostsAwaitingModeration(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getPostsAwaitingModerationByForum(Long forumId, Pageable pageable) {
        // Verify forum exists
        if (!forumRepository.existsById(forumId)) {
            throw new ResourceNotFoundException("Forum", "id", forumId);
        }
        
        return postRepository.findPostsAwaitingModerationByForumId(forumId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        return mapToDto(post);
    }

    @Transactional
    public Post createPost(Long topicId, String content, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));
        
        // Check if topic is locked
        if (topic.getStatus() == Topic.TopicStatus.LOCKED) {
            throw new BadRequestException("Cannot reply to a locked topic");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Forum forum = topic.getForum();
        
        // Get client IP address
        String ipAddress = getClientIpAddress();
        
        // Create the post
        Post post = Post.builder()
                .content(content)
                .editCount(0)
                .posterIp(ipAddress)
                .status(forum.isModerated() ? Post.PostStatus.WAITING : Post.PostStatus.APPROVED)
                .topic(topic)
                .user(user)
                .forum(forum)
                .build();
        
        Post savedPost = postRepository.save(post);
        
        // Update topic and forum statistics
        if (savedPost.getStatus() == Post.PostStatus.APPROVED) {
            topic.setLastPost(savedPost);
            topic.setReplyCount(topic.getReplyCount() + 1);
            topicRepository.save(topic);
            
            forum.setPostCount(forum.getPostCount() + 1);
            forum.setLastPost(savedPost);
            forumRepository.save(forum);
            
            // Update user post count
            user.setPostCount(user.getPostCount() + 1);
            userRepository.save(user);
        }
        
        return savedPost;
    }

    @Transactional
    public PostDto createPostWithAttachments(Long topicId, String content, Long userId, List<Long> attachmentIds) {
        Post post = createPost(topicId, content, userId);
        
        // Associate attachments with the post
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            attachmentService.associateAttachmentsWithPost(post.getId(), attachmentIds);
        }
        
        return mapToDto(post);
    }

    @Transactional
    public PostDto updatePost(Long id, String content, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        // Check if user is the post creator or has admin/moderator rights
        if (!post.getUser().getId().equals(userId) && 
                !forumService.isModerator(post.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to update this post");
        }
        
        post.setContent(content);
        post.setEditCount(post.getEditCount() + 1);
        post.setLastEditTime(LocalDateTime.now());
        
        Post updatedPost = postRepository.save(post);
        return mapToDto(updatedPost);
    }

    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        // Check if user is the post creator or has admin/moderator rights
        if (!post.getUser().getId().equals(userId) && 
                !forumService.isModerator(post.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to delete this post");
        }
        
        // If this is the first post of a topic, delete the entire topic
        if (post.getTopic().getFirstPost() != null && 
                post.getTopic().getFirstPost().getId().equals(post.getId())) {
            topicRepository.delete(post.getTopic());
            return;
        }
        
        // Delete attachments
        attachmentService.deleteAttachmentsByPostId(post.getId());
        
        // Delete the post
        postRepository.delete(post);
        
        // Update topic and forum statistics
        if (post.getStatus() == Post.PostStatus.APPROVED) {
            Topic topic = post.getTopic();
            topic.setReplyCount(topic.getReplyCount() - 1);
            
            // If this was the last post, find the new last post
            if (topic.getLastPost() != null && topic.getLastPost().getId().equals(post.getId())) {
                // Find the new last post
                List<Post> posts = postRepository.findByTopicIdOrderByCreatedAtAsc(topic.getId(), Pageable.ofSize(1))
                        .getContent();
                if (!posts.isEmpty()) {
                    topic.setLastPost(posts.get(0));
                } else {
                    topic.setLastPost(null);
                }
            }
            
            topicRepository.save(topic);
            
            // Update forum post count and last post if needed
            Forum forum = post.getForum();
            forum.setPostCount(forum.getPostCount() - 1);
            
            // If this was the last post in the forum, find the new last post
            if (forum.getLastPost() != null && forum.getLastPost().getId().equals(post.getId())) {
                // Find the new last post
                // This is a simplification - in a real app, you'd need a more efficient query
                forum.setLastPost(null);
            }
            
            forumRepository.save(forum);
            
            // Update user post count
            User user = post.getUser();
            user.setPostCount(Math.max(0, user.getPostCount() - 1));
            userRepository.save(user);
        }
    }

    @Transactional
    public void approvePost(Long id, Long moderatorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        // Check if user has moderator rights
        if (!forumService.isModerator(post.getForum().getId(), moderatorId)) {
            throw new ForbiddenException("You don't have permission to approve this post");
        }
        
        if (post.getStatus() != Post.PostStatus.WAITING) {
            throw new BadRequestException("Post is not awaiting moderation");
        }
        
        post.setStatus(Post.PostStatus.APPROVED);
        Post approvedPost = postRepository.save(post);
        
        // Update topic and forum statistics
        Topic topic = post.getTopic();
        topic.setReplyCount(topic.getReplyCount() + 1);
        
        // If this is a newer post than the current last post
        if (topic.getLastPost() == null || 
                approvedPost.getCreatedAt().isAfter(topic.getLastPost().getCreatedAt())) {
            topic.setLastPost(approvedPost);
        }
        
        topicRepository.save(topic);
        
        // Update forum statistics
        Forum forum = post.getForum();
        forum.setPostCount(forum.getPostCount() + 1);
        
        // If this is a newer post than the current last post
        if (forum.getLastPost() == null || 
                approvedPost.getCreatedAt().isAfter(forum.getLastPost().getCreatedAt())) {
            forum.setLastPost(approvedPost);
        }
        
        forumRepository.save(forum);
        
        // Update user post count
        User user = post.getUser();
        user.setPostCount(user.getPostCount() + 1);
        userRepository.save(user);
    }

    @Transactional
    public void rejectPost(Long id, Long moderatorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        // Check if user has moderator rights
        if (!forumService.isModerator(post.getForum().getId(), moderatorId)) {
            throw new ForbiddenException("You don't have permission to reject this post");
        }
        
        if (post.getStatus() != Post.PostStatus.WAITING) {
            throw new BadRequestException("Post is not awaiting moderation");
        }
        
        post.setStatus(Post.PostStatus.REJECTED);
        postRepository.save(post);
    }

    @Transactional
    public String getQuotedPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        return String.format("[quote=%s]%s[/quote]", post.getUser().getUsername(), post.getContent());
    }

    // Helper method to get client IP address
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ip = attributes.getRequest().getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = attributes.getRequest().getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = attributes.getRequest().getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = attributes.getRequest().getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // Ignore
        }
        return "0.0.0.0";
    }

    // Helper method to map Post entity to PostDto
    private PostDto mapToDto(Post post) {
        List<AttachmentDto> attachmentDtos = post.getAttachments().stream()
                .map(attachment -> AttachmentDto.builder()
                        .id(attachment.getId())
                        .filename(attachment.getFilename())
                        .filesize(attachment.getFilesize())
                        .contentType(attachment.getContentType())
                        .downloadCount(attachment.getDownloadCount())
                        .postId(attachment.getPost().getId())
                        .userId(attachment.getUser().getId())
                        .build())
                .collect(Collectors.toList());
        
        return PostDto.builder()
                .id(post.getId())
                .content(post.getContent())
                .editCount(post.getEditCount())
                .lastEditTime(post.getLastEditTime())
                .status(post.getStatus().name())
                .topicId(post.getTopic().getId())
                .forumId(post.getForum().getId())
                .user(UserSummaryDto.builder()
                        .id(post.getUser().getId())
                        .username(post.getUser().getUsername())
                        .avatarUrl(post.getUser().getAvatarUrl())
                        .build())
                .createdAt(post.getCreatedAt())
                .attachments(attachmentDtos)
                .build();
    }
}