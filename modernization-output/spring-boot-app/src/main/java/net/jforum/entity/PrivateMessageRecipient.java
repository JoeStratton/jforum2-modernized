package net.jforum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "private_message_recipients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageRecipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private PrivateMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false)
    private boolean flagged;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FolderType folder;

    public enum FolderType {
        INBOX, SENT, SAVED, TRASH
    }
}