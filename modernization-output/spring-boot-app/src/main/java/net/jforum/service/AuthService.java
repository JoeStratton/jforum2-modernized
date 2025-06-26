package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.AuthenticationRequestDto;
import net.jforum.dto.AuthenticationResponseDto;
import net.jforum.dto.UserDto;
import net.jforum.dto.UserRegistrationDto;
import net.jforum.entity.User;
import net.jforum.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponseDto register(UserRegistrationDto registrationDto) {
        User user = userService.registerUser(
                registrationDto.getUsername(),
                registrationDto.getEmail(),
                registrationDto.getPassword(),
                registrationDto.getFirstName(),
                registrationDto.getLastName()
        );

        String jwt = jwtService.generateToken(user);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .registrationDate(user.getRegistrationDate())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .build();

        return AuthenticationResponseDto.builder()
                .token(jwt)
                .user(userDto)
                .build();
    }

    public AuthenticationResponseDto authenticate(AuthenticationRequestDto authRequest) {
        // This will throw an exception if authentication fails
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        User user = (User) userService.loadUserByUsername(authRequest.getUsername());
        
        // Update last visit
        userService.updateLastVisit(user.getId());

        String jwt = jwtService.generateToken(user);

        UserDto userDto = UserDto.builder()
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

        return AuthenticationResponseDto.builder()
                .token(jwt)
                .user(userDto)
                .build();
    }
}