package com.thread.Igniter.follow.repository;

import com.thread.Igniter.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow,Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId,Long followingId);
    Long countByFollowingId(Long id);
    Long countByFollowerId(Long id);
    List<Follow> findAllByFollowingId(Long id);
    List<Follow> findAllByFollowerId(Long id);
}
