package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.PostDto;
import net.jforum.dto.PostSummaryDto;
import net.jforum.dto.TopicDto;
import net.jforum.dto.UserSummaryDto;
import net.jforum.entity.Forum;
import net.jforum.entity.Post;
import net.jforum.entity.Topic;
import net.jforum.entity.User;
import net.jforum.exception.BadRequestException;
import net.jforum.exception.ForbiddenException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final ForumRepository forumRepository;
    private final UserRepository userRepository;
    private final ForumService forumService;
    private final PostService postService;

    @Transactional(readOnly = true)
    public Page<TopicDto> getTopicsByForum(Long forumId, Pageable pageable) {
        // Verify forum exists
        if (!forumRepository.existsById(forumId)) {
            throw new ResourceNotFoundException("Forum", "id", forumId);
        }
        
        return topicRepository.findByForumIdOrderByTypeDescCreatedAtDesc(forumId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<TopicDto> getRecentTopics(Pageable pageable) {
        return topicRepository.findRecentTopics(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<TopicDto> getHotTopics(int minViews, Pageable pageable) {
        return topicRepository.findHotTopics(minViews, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<TopicDto> getTopicsByUser(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        return topicRepository.findByUserId(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<TopicDto> getWatchedTopics(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        return topicRepository.findWatchedTopics(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public TopicDto getTopicById(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        // Increment view count
        topic.setViewCount(topic.getViewCount() + 1);
        topicRepository.save(topic);
        
        return mapToDto(topic);
    }

    @Transactional
    public TopicDto createTopic(Long forumId, String title, String content, Topic.TopicType type, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Create the topic
        Topic topic = Topic.builder()
                .title(title)
                .viewCount(0)
                .replyCount(0)
                .status(Topic.TopicStatus.UNLOCKED)
                .type(type)
                .moderated(forum.isModerated())
                .forum(forum)
                .user(user)
                .build();
        
        Topic savedTopic = topicRepository.save(topic);
        
        // Create the first post
        Post firstPost = postService.createPost(savedTopic.getId(), content, userId);
        
        // Update the topic with the first post
        savedTopic.setFirstPost(firstPost);
        savedTopic.setLastPost(firstPost);
        Topic updatedTopic = topicRepository.save(savedTopic);
        
        // Update forum statistics
        forumService.incrementTopicCount(forumId);
        forumService.incrementPostCount(forumId);
        forumService.updateLastPost(forumId, firstPost);
        
        return mapToDto(updatedTopic);
    }

    @Transactional
    public TopicDto updateTopic(Long id, String title, Topic.TopicType type, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        // Check if user is the topic creator or has admin/moderator rights
        if (!topic.getUser().getId().equals(userId) && 
                !forumService.isModerator(topic.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to update this topic");
        }
        
        if (title != null) {
            topic.setTitle(title);
        }
        
        if (type != null) {
            topic.setType(type);
        }
        
        Topic updatedTopic = topicRepository.save(topic);
        return mapToDto(updatedTopic);
    }

    @Transactional
    public void deleteTopic(Long id, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        // Check if user is the topic creator or has admin/moderator rights
        if (!topic.getUser().getId().equals(userId) && 
                !forumService.isModerator(topic.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to delete this topic");
        }
        
        // Delete all posts in the topic
        topic.getPosts().forEach(post -> postService.deletePost(post.getId(), userId));
        
        // Delete the topic
        topicRepository.delete(topic);
    }

    @Transactional
    public void lockTopic(Long id, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        // Check if user has admin/moderator rights
        if (!forumService.isModerator(topic.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to lock this topic");
        }
        
        topic.setStatus(Topic.TopicStatus.LOCKED);
        topicRepository.save(topic);
    }

    @Transactional
    public void unlockTopic(Long id, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        // Check if user has admin/moderator rights
        if (!forumService.isModerator(topic.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to unlock this topic");
        }
        
        topic.setStatus(Topic.TopicStatus.UNLOCKED);
        topicRepository.save(topic);
    }

    @Transactional
    public void moveTopic(Long id, Long destinationForumId, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        Forum destinationForum = forumRepository.findById(destinationForumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", destinationForumId));
        
        // Check if user has admin/moderator rights
        if (!forumService.isModerator(topic.getForum().getId(), userId)) {
            throw new ForbiddenException("You don't have permission to move this topic");
        }
        
        // Update post forum references
        for (Post post : topic.getPosts()) {
            post.setForum(destinationForum);
        }
        
        // Update topic's forum
        Long oldForumId = topic.getForum().getId();
        topic.setForum(destinationForum);
        topicRepository.save(topic);
        
        // Update forum statistics for both old and new forums
        // TODO: Implement proper statistics update for both forums
    }

    @Transactional
    public void watchTopic(Long id, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        topic.getWatchers().add(user);
        topicRepository.save(topic);
    }

    @Transactional
    public void unwatchTopic(Long id, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        topic.getWatchers().remove(user);
        topicRepository.save(topic);
    }

    @Transactional
    public boolean isWatching(Long id, Long userId) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return topic.getWatchers().contains(user);
    }

    @Transactional
    public void incrementReplyCount(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        topic.setReplyCount(topic.getReplyCount() + 1);
        topicRepository.save(topic);
    }

    @Transactional
    public void updateLastPost(Long id, Post post) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        
        topic.setLastPost(post);
        topicRepository.save(topic);
    }

    // Helper method to map Topic entity to TopicDto
    private TopicDto mapToDto(Topic topic) {
        TopicDto dto = TopicDto.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .viewCount(topic.getViewCount())
                .replyCount(topic.getReplyCount())
                .status(topic.getStatus().name())
                .type(topic.getType().name())
                .forumId(topic.getForum().getId())
                .user(UserSummaryDto.builder()
                        .id(topic.getUser().getId())
                        .username(topic.getUser().getUsername())
                        .avatarUrl(topic.getUser().getAvatarUrl())
                        .build())
                .createdAt(topic.getCreatedAt())
                .build();
        
        // Add last post info if available
        if (topic.getLastPost() != null) {
            Post lastPost = topic.getLastPost();
            PostSummaryDto postSummaryDto = PostSummaryDto.builder()
                    .id(lastPost.getId())
                    .topicId(lastPost.getTopic().getId())
                    .topicTitle(lastPost.getTopic().getTitle())
                    .createdAt(lastPost.getCreatedAt())
                    .user(UserSummaryDto.builder()
                            .id(lastPost.getUser().getId())
                            .username(lastPost.getUser().getUsername())
                            .avatarUrl(lastPost.getUser().getAvatarUrl())
                            .build())
                    .build();
            dto.setLastPost(postSummaryDto);
        }
        
        return dto;
    }
}