package com.thread.Igniter.comment.repository;

import com.thread.Igniter.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByThreadId(Long threadId);

    void deleteAllByParentCommentId(Long parentCommentId);
    long countByParentCommentId(Long parentCommentId);
}
