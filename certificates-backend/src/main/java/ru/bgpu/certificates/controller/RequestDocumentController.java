package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.dto.GenerateDocumentRequest;
import ru.bgpu.certificates.service.RequestDocumentService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/request-documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RequestDocumentController {

    private final RequestDocumentService requestDocumentService;

    @PostMapping("/common")
    public ResponseEntity<byte[]> generateCommonDocument(
            @RequestBody GenerateDocumentRequest request
    ) {
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
}