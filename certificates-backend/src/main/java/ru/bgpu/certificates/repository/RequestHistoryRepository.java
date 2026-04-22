package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bgpu.certificates.entity.RequestHistory;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
}