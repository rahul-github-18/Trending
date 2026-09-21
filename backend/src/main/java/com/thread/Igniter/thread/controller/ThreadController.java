package com.thread.Igniter.thread.controller;

import com.thread.Igniter.thread.dto.CursorPageResponse;
import com.thread.Igniter.thread.dto.ThreadRequestDTO;
import com.thread.Igniter.thread.dto.ThreadResponseDTO;
import com.thread.Igniter.thread.service.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {
    private final ThreadService threadService;

    @PostMapping("/create")
    public ThreadResponseDTO createThread(@RequestBody @Valid ThreadRequestDTO requestDTO,Authentication authentication){
        String username=authentication.getName();
        return threadService.createThread(requestDTO,username);
    }
    @GetMapping
    public Page<ThreadResponseDTO> getAllThreads(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long userId,
            Pageable pageable
    ){
        return threadService.getAllThreads(username, userId, pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<ThreadResponseDTO> getThreadsByUserId(
            @PathVariable Long userId,
            Pageable pageable
    ){
        return threadService.getThreadsByUserId(userId, pageable);
    }

    @GetMapping("/{id}")
    public ThreadResponseDTO getThreadById(@PathVariable Long id){
        return threadService.getThreadById(id);
    }
    @PutMapping("/{id}")
    public ThreadResponseDTO updateThread(@PathVariable Long id,
                                          @RequestBody @Valid ThreadRequestDTO request,
                                          Authentication authentication){
        String username=authentication.getName();
        return threadService.updateThread(id,request,username);
    }
    @DeleteMapping("/{id}")
    public String deleteThread(@PathVariable Long id, Authentication authentication){
        String username=authentication.getName();
        threadService.deleteThread(id,username);
        return "Thread is successfully deleted";
    }
    @GetMapping("/feed")
    public CursorPageResponse<ThreadResponseDTO> getFeed(Authentication authentication,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(defaultValue = "10") int size) {
        return threadService.getFeed(authentication.getName(), cursor,size);
    }
}