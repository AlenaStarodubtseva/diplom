package ru.bgpu.certificates.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessAccountDto {

    private Long id;

    private String login;

    private String fullName;

    private String role;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<Long> facultyIds;
}