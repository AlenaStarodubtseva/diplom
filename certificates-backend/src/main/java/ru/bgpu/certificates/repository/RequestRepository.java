package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bgpu.certificates.entity.Request;

public interface RequestRepository extends JpaRepository<Request, Long> {
}