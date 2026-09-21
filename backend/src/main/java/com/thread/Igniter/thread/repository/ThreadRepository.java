package com.thread.Igniter.thread.repository;

import com.thread.Igniter.thread.entity.Thread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface ThreadRepository extends JpaRepository<Thread,Long> {
    @Query("SELECT t FROM Thread t WHERE t.user.id = :userId")
    Page<Thread> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Thread t WHERE t.user.username = :username")
    Page<Thread> findByUserUsername(@Param("username") String username, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Thread t SET t.likeCount = t.likeCount + 1 WHERE t.id = :threadId")
    int incrementLikeCount(@Param("threadId") Long threadId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Thread t SET t.likeCount = t.likeCount - 1 WHERE t.id = :threadId AND t.likeCount > 0")
    int decrementLikeCount(@Param("threadId") Long threadId);

    @Query("""
    SELECT t
    FROM Thread t
    WHERE t.user.id = :userId
       OR t.user.id IN (
           SELECT f.following.id
           FROM Follow f
           WHERE f.follower.id = :userId
       )
    ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<Thread> findInitialFeedThreads(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT t
    FROM Thread t
    WHERE (
        t.user.id = :userId
        OR t.user.id IN (
            SELECT f.following.id
            FROM Follow f
            WHERE f.follower.id = :userId
        )
    )
    AND (
        t.createdAt < :cursorCreatedAt
        OR (
            t.createdAt = :cursorCreatedAt
            AND t.id < :cursorId
        )
    )
    ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<Thread> findFeedThreadsAfterCursor(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Thread t
    SET t.commentCount = t.commentCount + 1
    WHERE t.id = :threadId
    """)
    int incrementCommentCount(@Param("threadId") Long threadId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Thread t
    SET t.commentCount = t.commentCount - :count
    WHERE t.id = :threadId
      AND t.commentCount >= :count
    """)
    int decrementCommentCount(
            @Param("threadId") Long threadId,
            @Param("count") long count
    );
}
