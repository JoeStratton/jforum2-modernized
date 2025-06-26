package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.BanDto;
import net.jforum.dto.GroupDto;
import net.jforum.dto.ModerationLogDto;
import net.jforum.dto.UserSummaryDto;
import net.jforum.entity.*;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationLogRepository moderationLogRepository;
    private final UserRepository userRepository;
    private final ForumRepository forumRepository;
    private final TopicRepository topicRepository;
    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final BanRepository banRepository;

    @Transactional(readOnly = true)
    public Page<ModerationLogDto> getModerationLogs(Pageable pageable) {
        return moderationLogRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<ModerationLogDto> getModerationLogsByUser(Long userId, Pageable pageable) {
        return moderationLogRepository.findByUserId(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<ModerationLogDto> getModerationLogsByForum(Long forumId, Pageable pageable) {
        return moderationLogRepository.findByForumId(forumId, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void logModeration(ModerationLog.LogType type, String description, Long userId, Long postId, Long topicId, Long forumId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        ModerationLog log = ModerationLog.builder()
                .type(type)
                .description(description)
                .user(user)
                .build();
        
        if (postId != null) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            log.setPost(post);
        }
        
        if (topicId != null) {
            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));
            log.setTopic(topic);
        }
        
        if (forumId != null) {
            Forum forum = forumRepository.findById(forumId)
                    .orElseThrow(() -> new ResourceNotFoundException("Forum", "id", forumId));
            log.setForum(forum);
        }
        
        moderationLogRepository.save(log);
    }

    @Transactional
    public BanDto banUser(Long userId, String reason, LocalDateTime endDate, Long moderatorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Check if user is already banned
        banRepository.findByUserId(userId).ifPresent(ban -> {
            throw new IllegalStateException("User is already banned");
        });
        
        Ban ban = Ban.builder()
                .user(user)
                .reason(reason)
                .startDate(LocalDateTime.now())
                .endDate(endDate)
                .build();
        
        Ban savedBan = banRepository.save(ban);
        
        // Log the action
        logModeration(
                ModerationLog.LogType.USER_BAN,
                "User " + user.getUsername() + " was banned. Reason: " + reason,
                moderatorId,
                null,
                null,
                null
        );
        
        return mapBanToDto(savedBan);
    }

    @Transactional
    public void unbanUser(Long userId, Long moderatorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Ban ban = banRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ban not found for user"));
        
        banRepository.delete(ban);
        
        // Log the action
        logModeration(
                ModerationLog.LogType.USER_UNBAN,
                "User " + user.getUsername() + " was unbanned.",
                moderatorId,
                null,
                null,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<BanDto> getActiveBans() {
        return banRepository.findActiveBans(LocalDateTime.now()).stream()
                .map(this::mapBanToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BanDto getBanByUserId(Long userId) {
        Ban ban = banRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ban not found for user"));
        
        return mapBanToDto(ban);
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::mapGroupToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupDto createGroup(String name, String description, Long parentId) {
        // Check if group with same name exists
        if (groupRepository.existsByName(name)) {
            throw new IllegalStateException("Group with this name already exists");
        }
        
        Group parent = null;
        if (parentId != null) {
            parent = groupRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group", "id", parentId));
        }
        
        Group group = Group.builder()
                .name(name)
                .description(description)
                .parent(parent)
                .build();
        
        Group savedGroup = groupRepository.save(group);
        return mapGroupToDto(savedGroup);
    }

    @Transactional
    public GroupDto updateGroup(Long id, String name, String description, Long parentId) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", id));
        
        // Check if new name is already used by another group
        if (!group.getName().equals(name) && groupRepository.existsByName(name)) {
            throw new IllegalStateException("Group with this name already exists");
        }
        
        group.setName(name);
        group.setDescription(description);
        
        if (parentId != null && !parentId.equals(group.getParent() != null ? group.getParent().getId() : null)) {
            Group parent = groupRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group", "id", parentId));
            group.setParent(parent);
        } else if (parentId == null) {
            group.setParent(null);
        }
        
        Group updatedGroup = groupRepository.save(group);
        return mapGroupToDto(updatedGroup);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", id));
        
        // Check if group has users or subgroups
        if (!group.getSubgroups().isEmpty()) {
            throw new IllegalStateException("Cannot delete group with subgroups");
        }
        
        groupRepository.delete(group);
    }

    @Transactional
    public void addUserToGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        
        user.getGroups().add(group);
        userRepository.save(user);
    }

    @Transactional
    public void removeUserFromGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        
        user.getGroups().remove(group);
        userRepository.save(user);
    }

    // Helper methods for mapping entities to DTOs
    private ModerationLogDto mapToDto(ModerationLog log) {
        return ModerationLogDto.builder()
                .id(log.getId())
                .type(log.getType().name())
                .description(log.getDescription())
                .user(UserSummaryDto.builder()
                        .id(log.getUser().getId())
                        .username(log.getUser().getUsername())
                        .avatarUrl(log.getUser().getAvatarUrl())
                        .build())
                .postId(log.getPost() != null ? log.getPost().getId() : null)
                .topicId(log.getTopic() != null ? log.getTopic().getId() : null)
                .forumId(log.getForum() != null ? log.getForum().getId() : null)
                .createdAt(log.getCreatedAt())
                .build();
    }

    private BanDto mapBanToDto(Ban ban) {
        return BanDto.builder()
                .id(ban.getId())
                .userId(ban.getUser() != null ? ban.getUser().getId() : null)
                .username(ban.getUser() != null ? ban.getUser().getUsername() : null)
                .ipAddress(ban.getIpAddress())
                .email(ban.getEmail())
                .startDate(ban.getStartDate())
                .endDate(ban.getEndDate())
                .reason(ban.getReason())
                .build();
    }

    private GroupDto mapGroupToDto(Group group) {
        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .parentId(group.getParent() != null ? group.getParent().getId() : null)
                .build();
    }
}