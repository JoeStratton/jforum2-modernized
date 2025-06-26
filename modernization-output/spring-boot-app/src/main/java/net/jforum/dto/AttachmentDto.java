package net.jforum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    
    private Long id;
    
    private String filename;
    
    private long filesize;
    
    private String contentType;
    
    private int downloadCount;
    
    private Long postId;
    
    private Long userId;
}