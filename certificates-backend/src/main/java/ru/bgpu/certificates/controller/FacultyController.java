package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.entity.Faculty;
import ru.bgpu.certificates.repository.FacultyRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/faculties")
@RequiredArgsConstructor
@CrossOrigin
public class FacultyController {

    private final FacultyRepository facultyRepository;

    @GetMapping
    public List<Faculty> getAll() {
        return facultyRepository.findAll();
    }

    @GetMapping("/{id}")
    public Faculty getById(@PathVariable Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Факультет не найден"));
    }

    @PostMapping
    public Faculty create(@RequestBody Faculty faculty) {
        faculty.setId(null);
        faculty.setCreatedAt(LocalDateTime.now());
        faculty.setUpdatedAt(LocalDateTime.now());

        if (faculty.getIsActive() == null) {
            faculty.setIsActive(true);
        }

        if (faculty.getNextRegistrationNumber() == null) {
            faculty.setNextRegistrationNumber(1);
        }

        return facultyRepository.save(faculty);
    }

    @PutMapping("/{id}")
    public Faculty update(@PathVariable Long id, @RequestBody Faculty updatedFaculty) {
        Faculty existing = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Факультет не найден"));

        existing.setCode(updatedFaculty.getCode());
        existing.setName(updatedFaculty.getName());
        existing.setIsActive(updatedFaculty.getIsActive());
        existing.setNextRegistrationNumber(updatedFaculty.getNextRegistrationNumber());
        existing.setUpdatedAt(LocalDateTime.now());

        return facultyRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        facultyRepository.deleteById(id);
    }
}