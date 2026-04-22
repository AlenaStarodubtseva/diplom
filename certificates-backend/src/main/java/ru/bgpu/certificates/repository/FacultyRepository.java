package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bgpu.certificates.entity.Faculty;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
}