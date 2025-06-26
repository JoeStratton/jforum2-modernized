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
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime registrationDate;
    private LocalDateTime lastVisit;
    private boolean active;
    private boolean emailVerified;
    private String avatarUrl;
    private String signature;
    private int postCount;
}