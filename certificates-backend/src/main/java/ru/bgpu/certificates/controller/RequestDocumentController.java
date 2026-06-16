package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.dto.GenerateDocumentRequest;
import ru.bgpu.certificates.entity.AccessAccount;
import ru.bgpu.certificates.entity.Request;
import ru.bgpu.certificates.repository.AccessAccountFacultyRepository;
import ru.bgpu.certificates.repository.AccessAccountRepository;
import ru.bgpu.certificates.repository.RequestRepository;
import ru.bgpu.certificates.service.CertificatePrintService;
import ru.bgpu.certificates.service.RequestDocumentService;
import ru.bgpu.certificates.dto.CertificatePrintPreviewDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/request-documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RequestDocumentController {

    private final RequestDocumentService requestDocumentService;
    private final CertificatePrintService certificatePrintService;
    private final RequestRepository requestRepository;
    private final AccessAccountRepository accessAccountRepository;
    private final AccessAccountFacultyRepository accessAccountFacultyRepository;

    @PostMapping("/common")
    public ResponseEntity<byte[]> generateCommonDocument(
            @RequestBody GenerateDocumentRequest request
    ) {
        checkAccess(request);

        byte[] file = requestDocumentService.generateCommonDocument(request.getRequestIds());

        String filename = URLEncoder.encode(
                "Общий_документ.docx",
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
                .body(file);
    }

    @PostMapping("/print-certificates")
    public ResponseEntity<byte[]> generatePrintCertificates(
            @RequestBody GenerateDocumentRequest request
    ) {
        checkAccess(request);

        byte[] file = certificatePrintService.generatePrintCertificates(request.getRequestIds());

        String filename = URLEncoder.encode(
                "Справки_для_печати.xls",
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.ms-excel"
                ))
                .body(file);
    }

    @PostMapping("/print-certificates-preview")
    public List<CertificatePrintPreviewDto> previewPrintCertificates(
            @RequestBody GenerateDocumentRequest request
    ) {
        checkAccess(request);

        return certificatePrintService.buildPrintPreview(request.getRequestIds());
    }

    private void checkAccess(GenerateDocumentRequest request) {
        if ("ADMIN".equals(request.getActorRole())) {
            return;
        }

        if (!"SECRETARY".equals(request.getActorRole())) {
            return;
        }

        List<Long> availableFacultyIds = availableFacultyIds(request.getActorLogin());

        if (availableFacultyIds.isEmpty()) {
            throw new RuntimeException("Нет доступных факультетов для формирования документа");
        }

        List<Request> selectedRequests = requestRepository.findAllById(request.getRequestIds());

        boolean hasForbiddenRequest = selectedRequests.stream()
                .anyMatch(item -> item.getFacultyId() == null || !availableFacultyIds.contains(item.getFacultyId()));

        if (hasForbiddenRequest) {
            throw new RuntimeException("Нельзя сформировать документ по заявкам чужого факультета");
        }
    }

    private List<Long> availableFacultyIds(String actorLogin) {
        if (actorLogin == null || actorLogin.isBlank()) {
            return List.of();
        }

        AccessAccount account = accessAccountRepository
                .findByLoginIgnoreCase(actorLogin)
                .orElse(null);

        if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
            return List.of();
        }

        return accessAccountFacultyRepository
                .findByAccessAccountId(account.getId())
                .stream()
                .map(link -> link.getFaculty().getId())
                .toList();
    }
}