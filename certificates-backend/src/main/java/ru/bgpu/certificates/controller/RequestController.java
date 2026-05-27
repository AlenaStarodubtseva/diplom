package ru.bgpu.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bgpu.certificates.entity.AccessAccount;
import ru.bgpu.certificates.entity.Faculty;
import ru.bgpu.certificates.entity.Request;
import ru.bgpu.certificates.entity.RequestHistory;
import ru.bgpu.certificates.repository.AccessAccountFacultyRepository;
import ru.bgpu.certificates.repository.AccessAccountRepository;
import ru.bgpu.certificates.repository.FacultyRepository;
import ru.bgpu.certificates.repository.RequestHistoryRepository;
import ru.bgpu.certificates.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@CrossOrigin
public class RequestController {

    private final RequestRepository requestRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final FacultyRepository facultyRepository;
    private final AccessAccountRepository accessAccountRepository;
    private final AccessAccountFacultyRepository accessAccountFacultyRepository;

    @GetMapping
    public List<Request> getAll(
            @RequestParam(required = false) String actorLogin,
            @RequestParam(required = false) String actorRole
    ) {
        if ("ADMIN".equals(actorRole)) {
            return requestRepository.findAll();
        }

        if ("SECRETARY".equals(actorRole)) {
            List<Long> facultyIds = availableFacultyIds(actorLogin, actorRole);

            if (facultyIds.isEmpty()) {
                return List.of();
            }

            return requestRepository.findByFacultyIdIn(facultyIds);
        }

        return requestRepository.findAll();
    }

    @GetMapping("/{id}")
    public Request getById(
            @PathVariable Long id,
            @RequestParam(required = false) String actorLogin,
            @RequestParam(required = false) String actorRole
    ) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        checkAccess(request, actorLogin, actorRole);

        return request;
    }

    @PostMapping
    public Request create(@RequestBody Request request) {
        request.setId(null);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("NEW");
        }

        if (request.getIsDeleted() == null) {
            request.setIsDeleted(false);
        }

        if (request.getNeedScan() == null) {
            request.setNeedScan(false);
        }

        if (request.getCopiesCount() == null) {
            request.setCopiesCount(1);
        }

        Request saved = requestRepository.save(request);

        requestHistoryRepository.save(
                RequestHistory.builder()
                        .requestId(saved.getId())
                        .actionType("CREATE")
                        .oldStatus(null)
                        .newStatus(saved.getStatus())
                        .comment("Заявка создана")
                        .actorLogin("student")
                        .actorFullName(saved.getStudentFullName())
                        .actorRole("STUDENT")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    @PutMapping("/{id}")
    public Request update(
            @PathVariable Long id,
            @RequestBody Request updatedRequest
    ) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        existing.setFacultyId(updatedRequest.getFacultyId());
        existing.setCertificateType(updatedRequest.getCertificateType());
        existing.setPurpose(updatedRequest.getPurpose());
        existing.setCopiesCount(updatedRequest.getCopiesCount());
        existing.setPeriodFrom(updatedRequest.getPeriodFrom());
        existing.setPeriodTo(updatedRequest.getPeriodTo());
        existing.setNeedScan(updatedRequest.getNeedScan());
        existing.setStatus(updatedRequest.getStatus());
        existing.setStudentComment(updatedRequest.getStudentComment());
        existing.setSecretaryComment(updatedRequest.getSecretaryComment());
        existing.setStudentFullName(updatedRequest.getStudentFullName());
        existing.setGroupName(updatedRequest.getGroupName());
        existing.setCourse(updatedRequest.getCourse());
        existing.setFacultyName(updatedRequest.getFacultyName());
        existing.setRegistrationNumber(updatedRequest.getRegistrationNumber());
        existing.setRegistrationYear(updatedRequest.getRegistrationYear());
        existing.setRegisteredAt(updatedRequest.getRegisteredAt());
        existing.setIssuedAt(updatedRequest.getIssuedAt());
        existing.setAcceptedAt(updatedRequest.getAcceptedAt());
        existing.setCompletedAt(updatedRequest.getCompletedAt());
        existing.setArchivedAt(updatedRequest.getArchivedAt());
        existing.setIsDeleted(updatedRequest.getIsDeleted());
        existing.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(existing);
    }

    @PatchMapping("/{id}/accept")
    public Request acceptRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ActorRequest actor
    ) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        checkAccess(existing, actorLogin(actor, null), actorRole(actor, null));

        if (!"NEW".equals(existing.getStatus())) {
            throw new RuntimeException("Принять можно только новую заявку");
        }

        if (existing.getFacultyId() == null) {
            throw new RuntimeException("У заявки не указан факультет");
        }

        Faculty faculty = facultyRepository.findById(existing.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Факультет не найден"));

        Integer nextNumber = faculty.getNextRegistrationNumber();

        if (nextNumber == null || nextNumber < 1) {
            nextNumber = 1;
        }

        String oldStatus = existing.getStatus();

        existing.setStatus("ACCEPTED");
        existing.setRegistrationNumber(nextNumber);
        existing.setRegistrationYear(LocalDateTime.now().getYear());
        existing.setRegisteredAt(LocalDateTime.now());
        existing.setAcceptedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());

        Request saved = requestRepository.save(existing);

        faculty.setNextRegistrationNumber(nextNumber + 1);
        faculty.setUpdatedAt(LocalDateTime.now());
        facultyRepository.save(faculty);

        requestHistoryRepository.save(
                RequestHistory.builder()
                        .requestId(saved.getId())
                        .actionType("REGISTER")
                        .oldStatus(oldStatus)
                        .newStatus("ACCEPTED")
                        .comment("Заявка принята и зарегистрирована")
                        .actorLogin(actorLogin(actor, "secretary"))
                        .actorFullName(actorFullName(actor, "Секретарь"))
                        .actorRole(actorRole(actor, "SECRETARY"))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    @PatchMapping("/{id}/student-comment")
    public Request updateStudentComment(
            @PathVariable Long id,
            @RequestBody CommentRequest payload
    ) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        existing.setStudentComment(payload.getComment());
        existing.setUpdatedAt(LocalDateTime.now());

        Request saved = requestRepository.save(existing);

        requestHistoryRepository.save(
                RequestHistory.builder()
                        .requestId(saved.getId())
                        .actionType("STUDENT_COMMENT")
                        .oldStatus(saved.getStatus())
                        .newStatus(saved.getStatus())
                        .comment("Комментарий студента: " + safeComment(payload.getComment()))
                        .actorLogin(actorLogin(payload, "student"))
                        .actorFullName(actorFullName(payload, saved.getStudentFullName()))
                        .actorRole(actorRole(payload, "STUDENT"))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    @PatchMapping("/{id}/secretary-comment")
    public Request updateSecretaryComment(
            @PathVariable Long id,
            @RequestBody CommentRequest payload
    ) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        checkAccess(existing, actorLogin(payload, null), actorRole(payload, null));

        existing.setSecretaryComment(payload.getComment());
        existing.setUpdatedAt(LocalDateTime.now());

        Request saved = requestRepository.save(existing);

        requestHistoryRepository.save(
                RequestHistory.builder()
                        .requestId(saved.getId())
                        .actionType("SECRETARY_COMMENT")
                        .oldStatus(saved.getStatus())
                        .newStatus(saved.getStatus())
                        .comment("Комментарий секретаря: " + safeComment(payload.getComment()))
                        .actorLogin(actorLogin(payload, "secretary"))
                        .actorFullName(actorFullName(payload, "Секретарь"))
                        .actorRole(actorRole(payload, "SECRETARY"))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    @PatchMapping("/{id}/status")
    public Request updateStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest payload
    ) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        checkAccess(existing, actorLogin(payload, null), actorRole(payload, null));

        String oldStatus = existing.getStatus();

        existing.setStatus(payload.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        if ("READY".equals(payload.getStatus()) && existing.getCompletedAt() == null) {
            existing.setCompletedAt(LocalDateTime.now());
        }

        if ("ARCHIVED".equals(payload.getStatus()) && existing.getArchivedAt() == null) {
            existing.setArchivedAt(LocalDateTime.now());
        }

        Request saved = requestRepository.save(existing);

        String historyComment = payload.getComment();

        if (historyComment == null || historyComment.isBlank()) {
            historyComment = "Статус изменён";
        }

        requestHistoryRepository.save(
                RequestHistory.builder()
                        .requestId(saved.getId())
                        .actionType("STATUS_CHANGE")
                        .oldStatus(oldStatus)
                        .newStatus(saved.getStatus())
                        .comment(historyComment)
                        .actorLogin(actorLogin(payload, "secretary"))
                        .actorFullName(actorFullName(payload, "Секретарь"))
                        .actorRole(actorRole(payload, "SECRETARY"))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    @PatchMapping("/{id}/cancel")
    public Request cancelRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ActorRequest actor
    ) {
        Request existing = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        String oldStatus = existing.getStatus();

        existing.setStatus("CANCELLED");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setArchivedAt(LocalDateTime.now());

        Request saved = requestRepository.save(existing);

        requestHistoryRepository.save(
                RequestHistory.builder()
                        .requestId(saved.getId())
                        .actionType("CANCEL")
                        .oldStatus(oldStatus)
                        .newStatus("CANCELLED")
                        .comment("Заявка отменена студентом")
                        .actorLogin(actorLogin(actor, "student"))
                        .actorFullName(actorFullName(actor, saved.getStudentFullName()))
                        .actorRole(actorRole(actor, "STUDENT"))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        requestRepository.deleteById(id);
    }

    private void checkAccess(Request request, String actorLogin, String actorRole) {
        if ("ADMIN".equals(actorRole)) {
            return;
        }

        if (!"SECRETARY".equals(actorRole)) {
            return;
        }

        if (request.getFacultyId() == null) {
            throw new RuntimeException("У заявки не указан факультет");
        }

        List<Long> facultyIds = availableFacultyIds(actorLogin, actorRole);

        if (!facultyIds.contains(request.getFacultyId())) {
            throw new RuntimeException("Нет доступа к заявке этого факультета");
        }
    }

    private List<Long> availableFacultyIds(String actorLogin, String actorRole) {
        if (!"SECRETARY".equals(actorRole)) {
            return List.of();
        }

        if (actorLogin == null || actorLogin.isBlank()) {
            return List.of();
        }

        AccessAccount account = accessAccountRepository
                .findByLoginIgnoreCase(actorLogin)
                .orElse(null);

        if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
            return List.of();
        }

        return accessAccountFacultyRepository
                .findByAccessAccountId(account.getId())
                .stream()
                .map(link -> link.getFaculty().getId())
                .toList();
    }

    private String actorLogin(ActorRequest actor, String fallback) {
        if (actor == null || actor.getActorLogin() == null || actor.getActorLogin().isBlank()) {
            return fallback;
        }

        return actor.getActorLogin();
    }

    private String actorFullName(ActorRequest actor, String fallback) {
        if (actor == null || actor.getActorFullName() == null || actor.getActorFullName().isBlank()) {
            return fallback;
        }

        return actor.getActorFullName();
    }

    private String actorRole(ActorRequest actor, String fallback) {
        if (actor == null || actor.getActorRole() == null || actor.getActorRole().isBlank()) {
            return fallback;
        }

        return actor.getActorRole();
    }

    private String safeComment(String comment) {
        return comment == null || comment.isBlank() ? "—" : comment;
    }

    public static class ActorRequest {
        private String actorLogin;
        private String actorFullName;
        private String actorRole;

        public String getActorLogin() {
            return actorLogin;
        }

        public void setActorLogin(String actorLogin) {
            this.actorLogin = actorLogin;
        }

        public String getActorFullName() {
            return actorFullName;
        }

        public void setActorFullName(String actorFullName) {
            this.actorFullName = actorFullName;
        }

        public String getActorRole() {
            return actorRole;
        }

        public void setActorRole(String actorRole) {
            this.actorRole = actorRole;
        }
    }

    public static class CommentRequest extends ActorRequest {
        private String comment;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public static class StatusRequest extends ActorRequest {
        private String status;
        private String comment;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}