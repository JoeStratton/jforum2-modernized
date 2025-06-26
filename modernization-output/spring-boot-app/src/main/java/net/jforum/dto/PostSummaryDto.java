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
public class PostSummaryDto {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private LocalDateTime createdAt;
    private UserSummaryDto user;
}