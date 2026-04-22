package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.entity.RequestHistory;
import ru.bgpu.certificates.repository.RequestHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/request-history")
@RequiredArgsConstructor
@CrossOrigin
public class RequestHistoryController {

    private final RequestHistoryRepository requestHistoryRepository;

    @GetMapping
    public List<RequestHistory> getAll() {
        return requestHistoryRepository.findAll();
    }

    @GetMapping("/{id}")
    public RequestHistory getById(@PathVariable Long id) {
        return requestHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись истории не найдена"));
    }

    @PostMapping
    public RequestHistory create(@RequestBody RequestHistory requestHistory) {
        requestHistory.setId(null);

        if (requestHistory.getCreatedAt() == null) {
            requestHistory.setCreatedAt(LocalDateTime.now());
        }

        return requestHistoryRepository.save(requestHistory);
    }

    @PutMapping("/{id}")
    public RequestHistory update(@PathVariable Long id, @RequestBody RequestHistory updatedHistory) {
        RequestHistory existing = requestHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись истории не найдена"));

        existing.setRequestId(updatedHistory.getRequestId());
        existing.setActionType(updatedHistory.getActionType());
        existing.setOldStatus(updatedHistory.getOldStatus());
        existing.setNewStatus(updatedHistory.getNewStatus());
        existing.setComment(updatedHistory.getComment());
        existing.setActorLogin(updatedHistory.getActorLogin());
        existing.setActorFullName(updatedHistory.getActorFullName());
        existing.setActorRole(updatedHistory.getActorRole());
        existing.setCreatedAt(updatedHistory.getCreatedAt());

        return requestHistoryRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        requestHistoryRepository.deleteById(id);
    }
}