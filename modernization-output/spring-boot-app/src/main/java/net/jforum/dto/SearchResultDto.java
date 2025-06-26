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
public class SearchResultDto {
    
    private List<PostDto> posts;
    
    private int totalResults;
    
    private int currentPage;
    
    private int totalPages;
}