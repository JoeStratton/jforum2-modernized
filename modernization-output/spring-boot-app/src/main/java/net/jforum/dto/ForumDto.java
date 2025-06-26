package net.jforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumDto {
    
    private Long id;
    
    @NotBlank(message = "Forum name is required")
    @Size(max = 150, message = "Forum name must be less than 150 characters")
    private String name;
    
    private String description;
    
    private int displayOrder;
    
    private boolean moderated;
    
    private int topicCount;
    
    private int postCount;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    private PostSummaryDto lastPost;
}