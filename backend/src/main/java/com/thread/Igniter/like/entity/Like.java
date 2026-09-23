package com.thread.Igniter.like.entity;

import com.thread.Igniter.thread.entity.Thread;
import com.thread.Igniter.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "thread_id"})
        }
)
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="thread_id",nullable = false)
    private Thread thread;
}
