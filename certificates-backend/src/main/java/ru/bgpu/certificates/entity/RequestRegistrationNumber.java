package ru.bgpu.certificates.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_registration_numbers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestRegistrationNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "registration_number", nullable = false)
    private Integer registrationNumber;

    @Column(name = "registration_year", nullable = false)
    private Integer registrationYear;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}