package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.bgpu.certificates.entity.AccessAccountFaculty;

import java.util.List;

@Repository
public interface AccessAccountFacultyRepository extends JpaRepository<AccessAccountFaculty, Long> {

    List<AccessAccountFaculty> findByAccessAccountId(Long accessAccountId);

    void deleteByAccessAccountId(Long accessAccountId);
}