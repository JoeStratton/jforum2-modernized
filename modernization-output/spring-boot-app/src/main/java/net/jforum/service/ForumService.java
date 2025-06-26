package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.ForumDto;
import net.jforum.dto.PostSummaryDto;
import net.jforum.dto.UserSummaryDto;
import net.jforum.entity.Category;
import net.jforum.entity.Forum;
import net.jforum.entity.Post;
import net.jforum.entity.User;
import net.jforum.exception.ConflictException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.CategoryRepository;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ForumDto> getAllForums() {
        return forumRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ForumDto> getForumsByCategory(Long categoryId) {
        return forumRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ForumDto getForumById(Long id) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", id));
        return mapToDto(forum);
    }

    @Transactional
    public ForumDto createForum(ForumDto forumDto) {
        Category category = categoryRepository.findById(forumDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", forumDto.getCategoryId()));

        Forum forum = Forum.builder()
                .name(forumDto.getName())
                .description(forumDto.getDescription())
                .displayOrder(forumDto.getDisplayOrder())
                .moderated(forumDto.isModerated())
                .topicCount(0)
                .postCount(0)
                .category(category)
                .build();

        Forum savedForum = forumRepository.save(forum);
        return mapToDto(savedForum);
    }

    @Transactional
    public ForumDto updateForum(Long id, ForumDto forumDto) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", id));

        // If category is changing, verify the new category exists
        if (!forum.getCategory().getId().equals(forumDto.getCategoryId())) {
            Category newCategory = categoryRepository.findById(forumDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", forumDto.getCategoryId()));
            forum.setCategory(newCategory);
        }

        forum.setName(forumDto.getName());
        forum.setDescription(forumDto.getDescription());
        forum.setDisplayOrder(forumDto.getDisplayOrder());
        forum.setModerated(forumDto.isModerated());

        Forum updatedForum = forumRepository.save(forum);
        return mapToDto(updatedForum);
    }

    @Transactional
    public void deleteForum(Long id) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", id));

        if (!forum.getTopics().isEmpty()) {
            throw new ConflictException("Cannot delete forum with topics. Remove topics first.");
        }

        forumRepository.delete(forum);
    }

    @Transactional
    public void addModerator(Long forumId, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        forum.getModerators().add(user);
        forumRepository.save(forum);
    }

    @Transactional
    public void removeModerator(Long forumId, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        forum.getModerators().remove(user);
        forumRepository.save(forum);
    }

    @Transactional
    public void watchForum(Long forumId, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        forum.getWatchers().add(user);
        forumRepository.save(forum);
    }

    @Transactional
    public void unwatchForum(Long forumId, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        forum.getWatchers().remove(user);
        forumRepository.save(forum);
    }

    @Transactional
    public boolean isWatching(Long forumId, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return forum.getWatchers().contains(user);
    }

    @Transactional
    public boolean isModerator(Long forumId, Long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return forum.getModerators().contains(user);
    }

    @Transactional
    public void incrementTopicCount(Long forumId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        forum.setTopicCount(forum.getTopicCount() + 1);
        forumRepository.save(forum);
    }

    @Transactional
    public void incrementPostCount(Long forumId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        forum.setPostCount(forum.getPostCount() + 1);
        forumRepository.save(forum);
    }

    @Transactional
    public void updateLastPost(Long forumId, Post post) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));

        forum.setLastPost(post);
        forumRepository.save(forum);
    }

    // Helper method to map Forum entity to ForumDto
    private ForumDto mapToDto(Forum forum) {
        ForumDto dto = ForumDto.builder()
                .id(forum.getId())
                .name(forum.getName())
                .description(forum.getDescription())
                .displayOrder(forum.getDisplayOrder())
                .moderated(forum.isModerated())
                .topicCount(forum.getTopicCount())
                .postCount(forum.getPostCount())
                .categoryId(forum.getCategory().getId())
                .build();

        // Add last post info if available
        if (forum.getLastPost() != null) {
            Post lastPost = forum.getLastPost();
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