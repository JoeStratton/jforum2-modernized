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
public class BanDto {
    
    private Long id;
    
    private Long userId;
    
    private String username;
    
    private String ipAddress;
    
    private String email;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private String reason;
}