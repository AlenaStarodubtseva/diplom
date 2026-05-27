package ru.bgpu.certificates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.bgpu.certificates.entity.AccessAccount;

import java.util.Optional;

@Repository
public interface AccessAccountRepository extends JpaRepository<AccessAccount, Long> {

    boolean existsByLoginIgnoreCase(String login);

    Optional<AccessAccount> findByLoginIgnoreCase(String login);
}