package com.thread.Igniter.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequestDTO {
    @NotBlank(message = "Comment cannot be empty")
    private String content;

    private Long parentCommentId;
}
