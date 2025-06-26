package net.jforum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "topics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String title;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "reply_count", nullable = false)
    private int replyCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicType type;

    @Column(nullable = false)
    private boolean moderated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id", nullable = false)
    private Forum forum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_post_id")
    private Post firstPost;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_post_id")
    private Post lastPost;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Post> posts = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "topics_watch",
            joinColumns = @JoinColumn(name = "topic_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> watchers = new HashSet<>();

    public enum TopicStatus {
        UNLOCKED, LOCKED
    }

    public enum TopicType {
        NORMAL, STICKY, ANNOUNCEMENT, POLL
    }
}