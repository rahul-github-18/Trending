package com.thread.Igniter.user.service;

import com.thread.Igniter.common.exception.ResourceAlreadyExistsException;
import com.thread.Igniter.user.dto.UserRequestDTO;
import com.thread.Igniter.user.dto.UserResponseDTO;
import com.thread.Igniter.user.dto.UserUpdateDTO;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Path profilePictureDirectory =
            Paths.get("uploads/profile");

    public UserResponseDTO createUser(UserRequestDTO request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email already exists"
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        user.setPassword(encodedPassword);
        user.setBio(request.getBio());

        User savedUser = userRepository.save(user);

        UserResponseDTO response = new UserResponseDTO();

        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setBio(savedUser.getBio());
        response.setProfilePicture(
                getProfilePictureUrl(savedUser.getProfilePicture())
        );
        response.setCreatedAt(savedUser.getCreatedAt());

        return response;
    }

    public UserResponseDTO getCurrentUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User Not Exists"));

        UserResponseDTO response = new UserResponseDTO();

        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setBio(user.getBio());
        response.setProfilePicture(
                getProfilePictureUrl(user.getProfilePicture())
        );
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }

    public UserResponseDTO uploadProfilePicture(
            String username,
            MultipartFile file) throws IOException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User Not Exists"));

        // Store the old profile picture path
        String oldProfilePicture = user.getProfilePicture();

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image/")) {

            throw new RuntimeException(
                    "Only image files are allowed"
            );
        }

        // Create directory if it doesn't exist
        Files.createDirectories(profilePictureDirectory);

        // Get file extension
        String extension = "";

        if (file.getOriginalFilename() != null) {

            String originalName = file.getOriginalFilename();

            int index = originalName.lastIndexOf(".");

            if (index != -1) {
                extension = originalName.substring(index);
            }
        }

        // Generate unique filename
        String fileName =
                UUID.randomUUID() + extension;

        // Create complete file path
        Path filePath =
                profilePictureDirectory.resolve(fileName);

        // Save new profile picture
        Files.copy(
                file.getInputStream(),
                filePath
        );

        // Delete old profile picture
        if (oldProfilePicture != null) {
            Files.deleteIfExists(
                    Paths.get(oldProfilePicture)
            );
        }

        // Update database with new picture path
        user.setProfilePicture(
                filePath.toString()
        );

        User savedUser =
                userRepository.save(user);

        // Build response
        UserResponseDTO response =
                new UserResponseDTO();

        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setBio(savedUser.getBio());
        response.setProfilePicture(
                getProfilePictureUrl(
                        savedUser.getProfilePicture()
                )
        );
        response.setCreatedAt(savedUser.getCreatedAt());

        return response;
    }

    public String getProfilePictureUrl(String profilePicture) {

        if (profilePicture == null) {
            return null;
        }

        String normalized = profilePicture.replace("\\", "/");
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    public UserResponseDTO updateUser(String username , UserUpdateDTO request){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->
                        new RuntimeException("Username Does Not exist"));

        if(userRepository.existsByUsernameAndIdNot(request.getUsername(), user.getId())){
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        if(userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())){
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setBio(request.getBio());
        user.setUpdatedAt(request.getUpdatedAt());
        User savedUser=userRepository.save(user);

        UserResponseDTO response=new UserResponseDTO();
        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setBio(savedUser.getBio());
        response.setCreatedAt(savedUser.getCreatedAt());
        response.setUpdatedAt(savedUser.getUpdatedAt());
        response.setProfilePicture(getProfilePictureUrl(savedUser.getProfilePicture()));
        return response;
    }
    public List<UserResponseDTO> searchUser(String username){
        return userRepository.findByUsernameContainingIgnoreCase(username)
                .stream()
                .map(user->{
                    UserResponseDTO response=new UserResponseDTO();
                    response.setId(user.getId());
                    response.setUsername(user.getUsername());
                    response.setBio(user.getBio());
                    response.setEmail(user.getEmail());
                    response.setProfilePicture(getProfilePictureUrl(user.getProfilePicture()));
                    return response;
                }).toList();
    }
}