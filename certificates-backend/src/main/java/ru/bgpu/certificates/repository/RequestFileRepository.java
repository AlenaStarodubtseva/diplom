package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bgpu.certificates.entity.RequestFile;

public interface RequestFileRepository extends JpaRepository<RequestFile, Long> {
}