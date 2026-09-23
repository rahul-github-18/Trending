package com.thread.Igniter.follow.controller;

import com.thread.Igniter.follow.service.FollowService;
import com.thread.Igniter.user.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{id}/follow")
    public String followUser(Authentication authentication, @PathVariable Long id){
        return followService.followUser(authentication.getName(),id);
    }
    @PostMapping("/{id}/unfollow")
    public String unfollowUser(Authentication authentication,@PathVariable Long id){
        return followService.unfollowUser(authentication.getName(),id);
    }
    @GetMapping("/{id}/following/status")
    public boolean isFollowing(Authentication authentication,@PathVariable Long id){
        return followService.isFollowing(authentication.getName(), id);
    }
    @GetMapping("/{targetUserId}/followers/count")
    public Long getFollowerCount(@PathVariable Long targetUserId){
        return followService.getFollowerCount(targetUserId);
    }
    @GetMapping("/{targetUserId}/following/count")
    public Long getFollowingCount(@PathVariable Long targetUserId){
        return followService.getFollowingCount(targetUserId);
    }
    @GetMapping("/{id}/followers")
    public List<UserResponseDTO> getFollowers(@PathVariable Long id){
        return followService.getFollowers(id);
    }
    @GetMapping("/{id}/following")
    public List<UserResponseDTO> getFollowing(@PathVariable Long id){
        return followService.getFollowing(id);
    }
}
