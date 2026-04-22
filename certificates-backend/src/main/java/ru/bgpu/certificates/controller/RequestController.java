package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.entity.Request;
import ru.bgpu.certificates.repository.RequestRepository;

import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@CrossOrigin
public class RequestController {

    private final RequestRepository requestRepository;

    @GetMapping
    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }
}