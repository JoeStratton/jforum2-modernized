package net.jforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TopicDto {
    
    private Long id;
    
    @NotBlank(message = "Topic title is required")
    @Size(max = 100, message = "Topic title must be less than 100 characters")
    private String title;
    
    private int viewCount;
    
    private int replyCount;
    
    private String status;
    
    private String type;
    
    @NotNull(message = "Forum ID is required")
    private Long forumId;
    
    private UserSummaryDto user;
    
    private LocalDateTime createdAt;
    
    private PostSummaryDto lastPost;
    
    private List<PostDto> posts;
}