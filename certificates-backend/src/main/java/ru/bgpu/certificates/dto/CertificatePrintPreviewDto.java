package ru.bgpu.certificates.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificatePrintPreviewDto {

    private Long requestId;

    private String registrationNumber;
    private String issueDate;

    private String studentFullName;
    private String birthDate;
    private String course;
    private String facultyName;
    private String direction;
    private String profile;
    private String groupName;
    private String educationForm;
    private String educationBasis;
    private String educationLevel;
    private String studyPeriod;
    private String purpose;
    private String enrollmentOrder;

    private String deanName;
    private String secretaryName;
}