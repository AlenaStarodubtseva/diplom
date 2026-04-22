package ru.bgpu.certificates.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;

    private String comment;

    @Column(name = "actor_login")
    private String actorLogin;

    @Column(name = "actor_full_name")
    private String actorFullName;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}