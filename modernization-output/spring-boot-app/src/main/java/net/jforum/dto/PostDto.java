package net.jforum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    
    private Long id;
    
    @NotBlank(message = "Post content is required")
    private String content;
    
    private int editCount;
    
    private LocalDateTime lastEditTime;
    
    private String status;
    
    private Long topicId;
    
    private Long forumId;
    
    private UserSummaryDto user;
    
    private LocalDateTime createdAt;
    
    private List<AttachmentDto> attachments;
}