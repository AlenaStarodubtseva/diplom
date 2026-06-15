package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.bgpu.certificates.entity.RequestRegistrationNumber;

import java.util.List;

@Repository
public interface RequestRegistrationNumberRepository extends JpaRepository<RequestRegistrationNumber, Long> {

    List<RequestRegistrationNumber> findByRequestIdOrderByRegistrationNumberAsc(Long requestId);

    List<RequestRegistrationNumber> findByRequestIdInOrderByRequestIdAscRegistrationNumberAsc(List<Long> requestIds);

    void deleteByRequestId(Long requestId);
}