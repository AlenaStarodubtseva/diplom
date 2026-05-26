package ru.bgpu.certificates.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bgpu.certificates.dto.AccessAccountDto;
import ru.bgpu.certificates.entity.AccessAccount;
import ru.bgpu.certificates.entity.AccessAccountFaculty;
import ru.bgpu.certificates.entity.Faculty;
import ru.bgpu.certificates.repository.AccessAccountFacultyRepository;
import ru.bgpu.certificates.repository.AccessAccountRepository;
import ru.bgpu.certificates.repository.FacultyRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessAccountService {

    private final AccessAccountRepository accessAccountRepository;
    private final AccessAccountFacultyRepository accessAccountFacultyRepository;
    private final FacultyRepository facultyRepository;

    @Transactional(readOnly = true)
    public List<AccessAccountDto> findAll() {
        return accessAccountRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AccessAccountDto create(AccessAccountDto dto) {
        if (accessAccountRepository.existsByLoginIgnoreCase(dto.getLogin())) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует");
        }

        AccessAccount account = AccessAccount.builder()
                .login(dto.getLogin())
                .fullName(dto.getFullName())
                .role(dto.getRole())
                .facultyCode(null)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AccessAccount saved = accessAccountRepository.save(account);

        saveFacultyLinks(saved, dto.getFacultyIds());

        return toDto(saved);
    }

    @Transactional
    public AccessAccountDto update(Long id, AccessAccountDto dto) {
        AccessAccount account = accessAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Доступ не найден"));

        account.setLogin(dto.getLogin());
        account.setFullName(dto.getFullName());
        account.setRole(dto.getRole());
        account.setFacultyCode(null);
        account.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        account.setUpdatedAt(LocalDateTime.now());

        AccessAccount saved = accessAccountRepository.save(account);

        accessAccountFacultyRepository.deleteByAccessAccountId(saved.getId());
        saveFacultyLinks(saved, dto.getFacultyIds());

        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        AccessAccount account = accessAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Доступ не найден"));

        if ("admin".equalsIgnoreCase(account.getLogin())) {
            throw new IllegalArgumentException("Нельзя удалить системного администратора");
        }

        accessAccountRepository.delete(account);
    }

    @Transactional
    public AccessAccountDto toggleActive(Long id) {
        AccessAccount account = accessAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Доступ не найден"));

        if ("admin".equalsIgnoreCase(account.getLogin())) {
            throw new IllegalArgumentException("Нельзя отключить системного администратора");
        }

        account.setIsActive(!Boolean.TRUE.equals(account.getIsActive()));
        account.setUpdatedAt(LocalDateTime.now());

        return toDto(accessAccountRepository.save(account));
    }

    private void saveFacultyLinks(AccessAccount account, List<Long> facultyIds) {
        if ("ADMIN".equals(account.getRole())) {
            return;
        }

        if (facultyIds == null || facultyIds.isEmpty()) {
            throw new IllegalArgumentException("Для секретаря нужно выбрать хотя бы один факультет");
        }

        for (Long facultyId : facultyIds) {
            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new IllegalArgumentException("Факультет не найден"));

            AccessAccountFaculty link = AccessAccountFaculty.builder()
                    .accessAccount(account)
                    .faculty(faculty)
                    .build();

            accessAccountFacultyRepository.save(link);
        }
    }

    private AccessAccountDto toDto(AccessAccount account) {
        List<Long> facultyIds = accessAccountFacultyRepository
                .findByAccessAccountId(account.getId())
                .stream()
                .map(link -> link.getFaculty().getId())
                .toList();

        return AccessAccountDto.builder()
                .id(account.getId())
                .login(account.getLogin())
                .fullName(account.getFullName())
                .role(account.getRole())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .facultyIds(facultyIds)
                .build();
    }
}