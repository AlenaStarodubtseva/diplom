package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.entity.RequestFile;
import ru.bgpu.certificates.repository.RequestFileRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/request-files")
@RequiredArgsConstructor
@CrossOrigin
public class RequestFileController {

    private final RequestFileRepository requestFileRepository;

    @GetMapping
    public List<RequestFile> getAll() {
        return requestFileRepository.findAll();
    }

    @GetMapping("/{id}")
    public RequestFile getById(@PathVariable Long id) {
        return requestFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
    }

    @PostMapping
    public RequestFile create(@RequestBody RequestFile requestFile) {
        requestFile.setId(null);

        if (requestFile.getCreatedAt() == null) {
            requestFile.setCreatedAt(LocalDateTime.now());
        }

        if (requestFile.getIsDeleted() == null) {
            requestFile.setIsDeleted(false);
        }

        return requestFileRepository.save(requestFile);
    }

    @PutMapping("/{id}")
    public RequestFile update(@PathVariable Long id, @RequestBody RequestFile updatedFile) {
        RequestFile existing = requestFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        existing.setRequestId(updatedFile.getRequestId());
        existing.setFileName(updatedFile.getFileName());
        existing.setStoragePath(updatedFile.getStoragePath());
        existing.setMimeType(updatedFile.getMimeType());
        existing.setFileSize(updatedFile.getFileSize());
        existing.setCreatedAt(updatedFile.getCreatedAt());
        existing.setExpiresAt(updatedFile.getExpiresAt());
        existing.setIsDeleted(updatedFile.getIsDeleted());

        return requestFileRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        requestFileRepository.deleteById(id);
    }
}