package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.dto.RequestRegistrationNumberDto;
import ru.bgpu.certificates.entity.RequestRegistrationNumber;
import ru.bgpu.certificates.repository.RequestRegistrationNumberRepository;

import java.util.List;

@RestController
@RequestMapping("/api/request-registration-numbers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RequestRegistrationNumberController {

    private final RequestRegistrationNumberRepository requestRegistrationNumberRepository;

    @GetMapping("/request/{requestId}")
    public List<RequestRegistrationNumberDto> getByRequestId(@PathVariable Long requestId) {
        return requestRegistrationNumberRepository.findByRequestIdOrderByRegistrationNumberAsc(requestId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping
    public List<RequestRegistrationNumberDto> getByRequestIds(@RequestParam List<Long> requestIds) {
        return requestRegistrationNumberRepository.findByRequestIdInOrderByRequestIdAscRegistrationNumberAsc(requestIds)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private RequestRegistrationNumberDto toDto(RequestRegistrationNumber item) {
        return RequestRegistrationNumberDto.builder()
                .id(item.getId())
                .requestId(item.getRequestId())
                .facultyId(item.getFacultyId())
                .registrationNumber(item.getRegistrationNumber())
                .registrationYear(item.getRegistrationYear())
                .createdAt(item.getCreatedAt())
                .build();
    }
}