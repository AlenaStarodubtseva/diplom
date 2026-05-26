package ru.bgpu.certificates.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import ru.bgpu.certificates.entity.Faculty;
import ru.bgpu.certificates.entity.Request;
import ru.bgpu.certificates.repository.FacultyRepository;
import ru.bgpu.certificates.repository.RequestRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RequestDocumentService {

    private final RequestRepository requestRepository;
    private final FacultyRepository facultyRepository;

    public byte[] generateCommonDocument(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            throw new IllegalArgumentException("Не выбраны заявки для формирования документа");
        }

        List<Request> requests = requestRepository.findAllById(requestIds);

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Заявки не найдены");
        }

        boolean hasNoStipendRequests = requests.stream()
                .anyMatch(request -> !"WITH_STIPEND".equals(request.getCertificateType()));

        if (hasNoStipendRequests) {
            throw new IllegalArgumentException(
                    "Общий документ можно сформировать только для справок с отметкой о стипендии"
            );
        }

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            createTitle(document);
            createFacultyLine(document, requests);
            createRequestsTable(document, requests);

            document.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при формировании Word-документа", e);
        }
    }

    private void createTitle(XWPFDocument document) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun titleRun = title.createRun();
        titleRun.setBold(true);
        titleRun.setFontFamily("Times New Roman");
        titleRun.setFontSize(14);

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        titleRun.setText("Заявка на справки от " + currentDate);
    }

    private void createFacultyLine(XWPFDocument document, List<Request> requests) {
        XWPFParagraph facultyParagraph = document.createParagraph();
        facultyParagraph.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun run = facultyParagraph.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(12);
        run.setBold(true);

        run.setText("Факультет: " + buildFacultyText(requests));
    }

    private String buildFacultyText(List<Request> requests) {
        Set<String> facultyNames = new LinkedHashSet<>();

        for (Request request : requests) {
            if (request.getFacultyId() == null) {
                continue;
            }

            facultyRepository.findById(request.getFacultyId())
                    .map(Faculty::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .ifPresent(facultyNames::add);
        }

        if (facultyNames.isEmpty()) {
            return "—";
        }

        return String.join(", ", facultyNames);
    }

    private void createRequestsTable(XWPFDocument document, List<Request> requests) {
        XWPFTable table = document.createTable(1, 5);

        XWPFTableRow header = table.getRow(0);

        setCellText(header.getCell(0), "Ф.И.О.", true);
        setCellText(header.getCell(1), "Курс, группа", true);
        setCellText(header.getCell(2), "Куда", true);
        setCellText(header.getCell(3), "Сколько", true);
        setCellText(header.getCell(4), "Примечание", true);

        for (Request request : requests) {
            XWPFTableRow row = table.createRow();

            setCellText(row.getCell(0), safe(request.getStudentFullName()), false);
            setCellText(row.getCell(1), buildCourseGroup(request), false);
            setCellText(row.getCell(2), safe(request.getPurpose()), false);
            setCellText(row.getCell(3), String.valueOf(request.getCopiesCount()), false);
            setCellText(row.getCell(4), buildNote(request), false);
        }
    }

    private String buildCourseGroup(Request request) {
        String course = request.getCourse() != null
                ? request.getCourse() + " курс"
                : "";

        String group = request.getGroupName() != null
                ? request.getGroupName()
                : "";

        if (!course.isBlank() && !group.isBlank()) {
            return course + ", " + group;
        }

        if (!course.isBlank()) {
            return course;
        }

        if (!group.isBlank()) {
            return group;
        }

        return "—";
    }

    private String buildNote(Request request) {
        if (request.getPeriodFrom() == null || request.getPeriodTo() == null) {
            return "С отметкой о стипендии";
        }

        long monthsCount = calculateMonthsCount(request.getPeriodFrom(), request.getPeriodTo());

        return monthsCount + " " + monthWord(monthsCount)
                + "\n("
                + formatRussianDate(request.getPeriodFrom())
                + " – "
                + formatRussianDate(request.getPeriodTo())
                + ")";
    }

    private long calculateMonthsCount(LocalDate periodFrom, LocalDate periodTo) {
        LocalDate from = periodFrom.withDayOfMonth(1);
        LocalDate to = periodTo.withDayOfMonth(1);

        return ChronoUnit.MONTHS.between(from, to) + 1;
    }

    private String monthWord(long monthsCount) {
        long lastTwoDigits = monthsCount % 100;
        long lastDigit = monthsCount % 10;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
            return "месяцев";
        }

        if (lastDigit == 1) {
            return "месяц";
        }

        if (lastDigit >= 2 && lastDigit <= 4) {
            return "месяца";
        }

        return "месяцев";
    }

    private String formatRussianDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "dd MMMM yyyy",
                new Locale("ru", "RU")
        );

        return date.format(formatter);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);

        String[] lines = text.split("\n");

        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        for (int i = 0; i < lines.length; i++) {
            XWPFRun run = paragraph.createRun();
            run.setFontFamily("Times New Roman");
            run.setFontSize(11);
            run.setBold(bold);
            run.setText(lines[i]);

            if (i < lines.length - 1) {
                run.addBreak();
            }
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}