package net.jforum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationLogDto {
    
    private Long id;
    
    private String type;
    
    private String description;
    
    private UserSummaryDto user;
    
    private Long postId;
    
    private Long topicId;
    
    private Long forumId;
    
    private LocalDateTime createdAt;
}