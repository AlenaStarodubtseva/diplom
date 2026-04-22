package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.entity.Request;
import ru.bgpu.certificates.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@CrossOrigin
public class RequestController {

    private final RequestRepository requestRepository;

    @GetMapping
    public List<Request> getAll() {
        return requestRepository.findAll();
    }

    @GetMapping("/{id}")
    public Request getById(@PathVariable Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
    }

    @PostMapping
    public Request create(@RequestBody Request request) {
        request.setId(null);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("NEW");
        }

        if (request.getIsDeleted() == null) {
            request.setIsDeleted(false);
        }

        if (request.getNeedScan() == null) {
            request.setNeedScan(false);
        }

        if (request.getCopiesCount() == null) {
            request.setCopiesCount(1);
        }

        return requestRepository.save(request);
    }

    @PutMapping("/{id}")
    public Request update(@PathVariable Long id, @RequestBody Request updatedRequest) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        existing.setFacultyId(updatedRequest.getFacultyId());
        existing.setCertificateType(updatedRequest.getCertificateType());
        existing.setPurpose(updatedRequest.getPurpose());
        existing.setCopiesCount(updatedRequest.getCopiesCount());
        existing.setPeriodFrom(updatedRequest.getPeriodFrom());
        existing.setPeriodTo(updatedRequest.getPeriodTo());
        existing.setNeedScan(updatedRequest.getNeedScan());
        existing.setStatus(updatedRequest.getStatus());
        existing.setStudentComment(updatedRequest.getStudentComment());
        existing.setSecretaryComment(updatedRequest.getSecretaryComment());
        existing.setStudentFullName(updatedRequest.getStudentFullName());
        existing.setGroupName(updatedRequest.getGroupName());
        existing.setCourse(updatedRequest.getCourse());
        existing.setFacultyName(updatedRequest.getFacultyName());
        existing.setRegistrationNumber(updatedRequest.getRegistrationNumber());
        existing.setRegistrationYear(updatedRequest.getRegistrationYear());
        existing.setRegisteredAt(updatedRequest.getRegisteredAt());
        existing.setIssuedAt(updatedRequest.getIssuedAt());
        existing.setAcceptedAt(updatedRequest.getAcceptedAt());
        existing.setCompletedAt(updatedRequest.getCompletedAt());
        existing.setArchivedAt(updatedRequest.getArchivedAt());
        existing.setIsDeleted(updatedRequest.getIsDeleted());
        existing.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(existing);
    }

    @PatchMapping("/{id}/student-comment")
    public Request updateStudentComment(@PathVariable Long id, @RequestBody CommentRequest payload) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        existing.setStudentComment(payload.getComment());
        existing.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(existing);
    }

    @PatchMapping("/{id}/secretary-comment")
    public Request updateSecretaryComment(@PathVariable Long id, @RequestBody CommentRequest payload) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        existing.setSecretaryComment(payload.getComment());
        existing.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(existing);
    }

    @PatchMapping("/{id}/status")
    public Request updateStatus(@PathVariable Long id, @RequestBody StatusRequest payload) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        existing.setStatus(payload.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        if ("ACCEPTED".equals(payload.getStatus()) && existing.getAcceptedAt() == null) {
            existing.setAcceptedAt(LocalDateTime.now());
        }

        if ("READY".equals(payload.getStatus()) && existing.getCompletedAt() == null) {
            existing.setCompletedAt(LocalDateTime.now());
        }

        return requestRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        requestRepository.deleteById(id);
    }

    public static class CommentRequest {
        private String comment;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public static class StatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}