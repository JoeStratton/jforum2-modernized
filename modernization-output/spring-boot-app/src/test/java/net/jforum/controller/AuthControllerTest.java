package net.jforum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jforum.dto.AuthenticationRequestDto;
import net.jforum.dto.AuthenticationResponseDto;
import net.jforum.dto.UserRegistrationDto;
import net.jforum.security.JwtService;
import net.jforum.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void register_ValidInput_ReturnsToken() throws Exception {
        // Arrange
        UserRegistrationDto registrationDto = new UserRegistrationDto(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User"
        );

        AuthenticationResponseDto responseDto = new AuthenticationResponseDto();
        responseDto.setToken("jwt-token");

        when(authService.register(any(UserRegistrationDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void authenticate_ValidCredentials_ReturnsToken() throws Exception {
        // Arrange
        AuthenticationRequestDto authRequest = new AuthenticationRequestDto(
                "testuser",
                "password123",
                false
        );

        AuthenticationResponseDto responseDto = new AuthenticationResponseDto();
        responseDto.setToken("jwt-token");

        when(authService.authenticate(any(AuthenticationRequestDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}