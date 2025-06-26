package net.jforum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWithForumsDto {
    
    private Long id;
    
    private String name;
    
    private int displayOrder;
    
    private boolean moderated;
    
    private List<ForumDto> forums;
}