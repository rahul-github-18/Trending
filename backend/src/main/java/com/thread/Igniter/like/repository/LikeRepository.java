package com.thread.Igniter.like.repository;
import com.thread.Igniter.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndThreadId(Long userId, Long threadId);
    Optional<Like> findByUserIdAndThreadId(Long userId, Long threadId);
    long countByThreadId(Long threadId);
    void deleteByThreadId(Long threadId);
}
