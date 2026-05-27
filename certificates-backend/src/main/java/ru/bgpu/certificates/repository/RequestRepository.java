package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bgpu.certificates.entity.Request;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByFacultyIdIn(List<Long> facultyIds);
}