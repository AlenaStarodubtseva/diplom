package ru.bgpu.certificates.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "access_account_faculties",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"access_account_id", "faculty_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessAccountFaculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_account_id", nullable = false)
    private AccessAccount accessAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;
}