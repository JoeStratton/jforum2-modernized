package net.jforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageDto {
    
    private Long id;
    
    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must be less than 255 characters")
    private String subject;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private String type;
    
    private UserSummaryDto fromUser;
    
    @NotEmpty(message = "At least one recipient is required")
    private List<Long> toUserIds;
    
    private Long parentMessageId;
}