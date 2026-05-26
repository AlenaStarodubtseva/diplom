package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.dto.AccessAccountDto;
import ru.bgpu.certificates.service.AccessAccountService;

import java.util.List;

@RestController
@RequestMapping("/api/access-accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccessAccountController {

    private final AccessAccountService accessAccountService;

    @GetMapping
    public List<AccessAccountDto> getAll() {
        return accessAccountService.findAll();
    }

    @PostMapping
    public AccessAccountDto create(@RequestBody AccessAccountDto dto) {
        return accessAccountService.create(dto);
    }

    @PutMapping("/{id}")
    public AccessAccountDto update(
            @PathVariable Long id,
            @RequestBody AccessAccountDto dto
    ) {
        return accessAccountService.update(id, dto);
    }

    @PatchMapping("/{id}/toggle-active")
    public AccessAccountDto toggleActive(@PathVariable Long id) {
        return accessAccountService.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        accessAccountService.delete(id);
    }
}