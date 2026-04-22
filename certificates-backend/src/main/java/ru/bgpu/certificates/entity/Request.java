package ru.bgpu.certificates.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id")
    private Long facultyId;

    @Column(name = "certificate_type")
    private String certificateType;

    private String purpose;

    @Column(name = "copies_count")
    private Integer copiesCount;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "need_scan")
    private Boolean needScan;

    private String status;

    @Column(name = "student_comment")
    private String studentComment;

    @Column(name = "secretary_comment")
    private String secretaryComment;

    @Column(name = "student_full_name")
    private String studentFullName;

    @Column(name = "group_name")
    private String groupName;

    private Integer course;

    @Column(name = "faculty_name")
    private String facultyName;

    @Column(name = "registration_number")
    private Integer registrationNumber;

    @Column(name = "registration_year")
    private Integer registrationYear;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}