package com.thread.Igniter.comment.controller;

import com.thread.Igniter.comment.dto.CommentRequestDTO;
import com.thread.Igniter.comment.dto.CommentResponseDTO;
import com.thread.Igniter.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{threadId}/comments")
    public CommentResponseDTO createComment(@PathVariable Long threadId,
                                            Authentication authentication,
                                            @RequestBody @Valid CommentRequestDTO request){
        return commentService.createComment(threadId,authentication.getName(),request);
    }
    @GetMapping("/{threadId}/comments")
    public List<CommentResponseDTO> getComments(@PathVariable Long threadId){
        return commentService.getComments(threadId);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponseDTO updateComment(@PathVariable Long commentId,
                                            Authentication authentication,
                                            @RequestBody @Valid CommentRequestDTO request){
        return commentService.updateComment(commentId, authentication.getName(), request);
    }
    @DeleteMapping("/comments/{commentId}")
    public String deleteComment(@PathVariable Long commentId,Authentication authentication){
        return commentService.deleteComment(commentId, authentication.getName());
    }
}
