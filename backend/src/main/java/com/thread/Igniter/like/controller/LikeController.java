package com.thread.Igniter.like.controller;
import com.thread.Igniter.like.dto.LikeResponseDTO;
import com.thread.Igniter.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    @PostMapping("/{id}/like")
    public LikeResponseDTO likeThread(@PathVariable Long id, Authentication authentication){
        return likeService.likeThread(id,authentication.getName());
    }
    @DeleteMapping("/{id}/like")
    public LikeResponseDTO  deleteLike(@PathVariable Long id,Authentication authentication){
        return likeService.deleteLike(id,authentication.getName());
    }
}
