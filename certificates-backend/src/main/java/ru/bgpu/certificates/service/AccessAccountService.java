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
        validateDto(dto, null);

        String login = dto.getLogin().trim();
        String fullName = dto.getFullName().trim();
        String role = dto.getRole().trim();

        AccessAccount account = AccessAccount.builder()
                .login(login)
                .fullName(fullName)
                .role(role)
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

        validateDto(dto, id);

        String login = dto.getLogin().trim();
        String fullName = dto.getFullName().trim();
        String role = dto.getRole().trim();

        account.setLogin(login);
        account.setFullName(fullName);
        account.setRole(role);
        account.setFacultyCode(null);
        account.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        account.setUpdatedAt(LocalDateTime.now());

        AccessAccount saved = accessAccountRepository.save(account);

        accessAccountFacultyRepository.deleteByAccessAccountId(saved.getId());
        accessAccountFacultyRepository.flush();

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

        accessAccountFacultyRepository.deleteByAccessAccountId(account.getId());
        accessAccountFacultyRepository.flush();

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

    private void validateDto(AccessAccountDto dto, Long editingId) {
        if (dto.getLogin() == null || dto.getLogin().isBlank()) {
            throw new IllegalArgumentException("Укажите логин");
        }

        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            throw new IllegalArgumentException("Укажите ФИО");
        }

        if (dto.getRole() == null || dto.getRole().isBlank()) {
            throw new IllegalArgumentException("Укажите роль");
        }

        if (!"ADMIN".equals(dto.getRole()) && !"SECRETARY".equals(dto.getRole())) {
            throw new IllegalArgumentException("Недопустимая роль");
        }

        AccessAccount duplicate = accessAccountRepository
                .findByLoginIgnoreCase(dto.getLogin().trim())
                .orElse(null);

        if (duplicate != null && !duplicate.getId().equals(editingId)) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует");
        }

        if ("SECRETARY".equals(dto.getRole())) {
            if (dto.getFacultyIds() == null || dto.getFacultyIds().isEmpty()) {
                throw new IllegalArgumentException("Для секретаря нужно выбрать хотя бы один факультет");
            }
        }
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