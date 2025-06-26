package net.jforum.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return false;
        }
        
        if (authentication.getPrincipal() instanceof net.jforum.entity.User) {
            net.jforum.entity.User user = (net.jforum.entity.User) authentication.getPrincipal();
            return userId.equals(user.getId());
        }
        
        return false;
    }
}