package com.thread.Igniter.testing.unit;

import com.thread.Igniter.common.exception.ResourceAlreadyExistsException;
import com.thread.Igniter.user.dto.UserRequestDTO;
import com.thread.Igniter.user.dto.UserResponseDTO;
import com.thread.Igniter.user.dto.UserUpdateDTO;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import com.thread.Igniter.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("alice");
        sampleUser.setEmail("alice@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setBio("Hello world");
        sampleUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully create a user")
    void testCreateUserSuccess() {
        UserRequestDTO request = new UserRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("plainPass");
        request.setBio("Hello world");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPass")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDTO response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("alice", response.getUsername());
        assertEquals("alice@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when username already exists")
    void testCreateUserDuplicateUsername() {
        UserRequestDTO request = new UserRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
    void testCreateUserDuplicateEmail() {
        UserRequestDTO request = new UserRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get current user by username")
    void testGetCurrentUserSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        UserResponseDTO response = userService.getCurrentUser("alice");

        assertNotNull(response);
        assertEquals("alice", response.getUsername());
        assertEquals("alice@example.com", response.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when current user is not found")
    void testGetCurrentUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getCurrentUser("nonexistent"));
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUserSuccess() {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setUsername("alice_new");
        updateDTO.setEmail("alice_new@example.com");
        updateDTO.setBio("Updated bio");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsernameAndIdNot("alice_new", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("alice_new@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDTO response = userService.updateUser("alice", updateDTO);

        assertNotNull(response);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when new username is taken")
    void testUpdateUserUsernameConflict() {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setUsername("taken_name");
        updateDTO.setEmail("alice@example.com");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsernameAndIdNot("taken_name", 1L)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> userService.updateUser("alice", updateDTO));
    }

    @Test
    @DisplayName("Should search users by username matching substring")
    void testSearchUser() {
        when(userRepository.findByUsernameContainingIgnoreCase("ali")).thenReturn(List.of(sampleUser));

        List<UserResponseDTO> results = userService.searchUser("ali");

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getUsername());
    }

    @Test
    @DisplayName("Should reject empty file on profile picture upload")
    void testUploadProfilePictureEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        assertThrows(RuntimeException.class, () -> userService.uploadProfilePicture("alice", emptyFile));
    }

    @Test
    @DisplayName("Should reject non-image file on profile picture upload")
    void testUploadProfilePictureNonImage() {
        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        assertThrows(RuntimeException.class, () -> userService.uploadProfilePicture("alice", textFile));
    }

    @Test
    @DisplayName("Should upload profile picture successfully")
    void testUploadProfilePictureSuccess() throws IOException {
        MockMultipartFile imageFile = new MockMultipartFile("file", "avatar.png", "image/png", "fake-image-bytes".getBytes());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDTO response = userService.uploadProfilePicture("alice", imageFile);

        assertNotNull(response);
        verify(userRepository).save(sampleUser);
    }
}
