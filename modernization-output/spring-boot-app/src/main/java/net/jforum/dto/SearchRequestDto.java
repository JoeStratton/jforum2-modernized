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
public class SearchRequestDto {
    
    private String keywords;
    
    private Long userId;
    
    private Long forumId;
    
    private String dateFrom;
    
    private String dateTo;
    
    private String sortBy;
}