package net.jforum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkDto {
    
    private Long id;
    
    private String type;
    
    private Long elementId;
    
    private String description;
    
    // Additional fields for display
    private String title;
}