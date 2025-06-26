package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.UserDto;
import net.jforum.entity.Group;
import net.jforum.entity.User;
import net.jforum.exception.ConflictException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.GroupRepository;
import net.jforum.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public User registerUser(String username, String email, String password, String firstName, String lastName) {
        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }

        // Check if email exists
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already in use");
        }

        // Get default user group
        Group userGroup = groupRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default user group not found"));

        // Create user
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .registrationDate(LocalDateTime.now())
                .isActive(true)
                .isEmailVerified(false) // Requires verification
                .activationKey(UUID.randomUUID().toString())
                .postCount(0)
                .groups(new HashSet<>(Set.of(userGroup)))
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable).map(this::mapToDto);
    }

    @Transactional
    public UserDto updateUser(Long id, String email, String firstName, String lastName, String avatarUrl, String signature) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // Update user fields
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new ConflictException("Email is already in use");
            }
            user.setEmail(email);
            user.setEmailVerified(false);
            user.setActivationKey(UUID.randomUUID().toString());
        }
        
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        
        if (lastName != null) {
            user.setLastName(lastName);
        }
        
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        
        if (signature != null) {
            user.setSignature(signature);
        }
        
        User updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void updatePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ConflictException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        user.setActivationKey(UUID.randomUUID().toString());
        userRepository.save(user);
        
        // TODO: Send password reset email
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        User user = userRepository.findByActivationKey(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired password reset token"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setActivationKey(null);
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByActivationKey(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired activation token"));
        
        user.setEmailVerified(true);
        user.setActivationKey(null);
        userRepository.save(user);
    }

    @Transactional
    public void updateLastVisit(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        user.setLastVisit(LocalDateTime.now());
        userRepository.save(user);
    }

    // Helper method to map User entity to UserDto
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .registrationDate(user.getRegistrationDate())
                .lastVisit(user.getLastVisit())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .postCount(user.getPostCount())
                .build();
    }
}