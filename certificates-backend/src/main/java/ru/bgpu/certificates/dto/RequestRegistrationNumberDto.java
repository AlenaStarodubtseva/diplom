package ru.bgpu.certificates.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestRegistrationNumberDto {

    private Long id;

    private Long requestId;

    private Long facultyId;

    private Integer registrationNumber;

    private Integer registrationYear;

    private LocalDateTime createdAt;
}