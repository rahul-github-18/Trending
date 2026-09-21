package com.thread.Igniter.follow.service;

import com.thread.Igniter.common.exception.BadRequestException;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.follow.entity.Follow;
import com.thread.Igniter.follow.repository.FollowRepository;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.user.dto.UserResponseDTO;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import com.thread.Igniter.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final   UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public String followUser(String username, Long targetUserId){
        User followFrom=userRepository.findByUsername(username)
                .orElseThrow(()->
                        new ResourceNotFoundException("User Not exists"));
        User followTo=userRepository.findById(targetUserId)
                .orElseThrow(()->
                        new ResourceNotFoundException("User Not Exists"));

        if((followFrom.getId()).equals( followTo.getId())){
            throw new BadRequestException("Cannot follow yourself");
        }

        if(followRepository.existsByFollowerIdAndFollowingId(followFrom.getId(),followTo.getId())){
            throw new BadRequestException("You already follow this account");
        }

        Follow follow=new Follow();
        follow.setFollower(followFrom);
        follow.setFollowing(followTo);
        followRepository.save(follow);
        notificationService.createNotification(
                followFrom.getUsername(),
                followTo.getId(),
                NotificationType.FOLLOW,
                followFrom.getUsername() + " started following you",followTo.getId());
        return "Followed Successfully";
    }
    @Transactional
    public String unfollowUser(String username,Long targetUserId){
        User unFollowFrom=userRepository.findByUsername(username)
                .orElseThrow(()->
                        new ResourceNotFoundException("User Not exists"));
        User unFollowTo=userRepository.findById(targetUserId)
                .orElseThrow(()->
                        new ResourceNotFoundException("User Not Exists"));

        if((unFollowFrom.getId()).equals( unFollowTo.getId())){
            throw new BadRequestException("Cannot unfollow yourself");
        }
        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(unFollowFrom.getId(), unFollowTo.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("You haven't followed this account"));
        followRepository.delete(follow);
        notificationService.deleteFollowNotification(unFollowFrom.getId(),unFollowTo.getId(),unFollowTo.getId());
        return "Unfollowed Successfully";
    }

    public boolean isFollowing(String username,Long targetUserId){
        User followFrom=userRepository.findByUsername(username)
                .orElseThrow(()->
                        new ResourceNotFoundException("User Not exists"));
        User followTo=userRepository.findById(targetUserId)
                .orElseThrow(()->
                        new ResourceNotFoundException("User Not Exists"));
        return followRepository.existsByFollowerIdAndFollowingId(followFrom.getId(),followTo.getId());

    }

    public Long getFollowerCount(Long id){
        return followRepository.countByFollowingId(id);
    }
    public Long getFollowingCount(Long id){
        return followRepository.countByFollowerId(id);
    }

    public List<UserResponseDTO> getFollowers(Long id) {

        List<Follow> follows = followRepository.findAllByFollowingId(id);

        List<UserResponseDTO> followers = follows.stream()
                .map(follow -> {
                    User user = follow.getFollower();
                    UserResponseDTO response = new UserResponseDTO();
                    response.setId(user.getId());
                    response.setUsername(user.getUsername());
                    response.setEmail(user.getEmail());
                    response.setBio(user.getBio());
                    response.setProfilePicture(
                            userService.getProfilePictureUrl(user.getProfilePicture())
                    );
                    response.setCreatedAt(user.getCreatedAt());

                    return response;
                })
                .toList();

        return followers;
    }

    public List<UserResponseDTO> getFollowing(Long id) {

        List<Follow> follows = followRepository.findAllByFollowerId(id);

        List<UserResponseDTO> following= follows.stream()
                .map(follow -> {
                    User user = follow.getFollowing();
                    UserResponseDTO response = new UserResponseDTO();
                    response.setId(user.getId());
                    response.setUsername(user.getUsername());
                    response.setEmail(user.getEmail());
                    response.setBio(user.getBio());
                    response.setProfilePicture(
                            userService.getProfilePictureUrl(user.getProfilePicture())
                    );
                    response.setCreatedAt(user.getCreatedAt());

                    return response;
                })
                .toList();

        return following;
    }
}
