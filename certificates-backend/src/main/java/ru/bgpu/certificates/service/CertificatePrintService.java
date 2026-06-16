package ru.bgpu.certificates.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import ru.bgpu.certificates.entity.Faculty;
import ru.bgpu.certificates.entity.Request;
import ru.bgpu.certificates.entity.RequestRegistrationNumber;
import ru.bgpu.certificates.repository.FacultyRepository;
import ru.bgpu.certificates.repository.RequestRegistrationNumberRepository;
import ru.bgpu.certificates.repository.RequestRepository;
import ru.bgpu.certificates.dto.CertificatePrintPreviewDto;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CertificatePrintService {

    private final RequestRepository requestRepository;
    private final FacultyRepository facultyRepository;
    private final RequestRegistrationNumberRepository requestRegistrationNumberRepository;

    private static final int CERTIFICATE_HEIGHT = 28;
    private static final int GAP_HEIGHT = 2;
    private static final int BLOCK_HEIGHT = CERTIFICATE_HEIGHT + GAP_HEIGHT;

    public byte[] generatePrintCertificates(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            throw new IllegalArgumentException("Не выбраны заявки для формирования справок");
        }

        List<Request> requests = requestRepository.findAllById(requestIds);

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Заявки не найдены");
        }

        Map<Long, List<RequestRegistrationNumber>> numbersByRequestId = loadRegistrationNumbers(requests);

        try (HSSFWorkbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            HSSFSheet sheet = workbook.createSheet("Справки");

            setupSheet(sheet);
            Styles styles = createStyles(workbook);

            int rowIndex = 0;

            for (Request request : requests) {
                List<RequestRegistrationNumber> numbers = numbersByRequestId.getOrDefault(request.getId(), List.of());

                if (numbers.isEmpty()) {
                    throw new IllegalArgumentException(
                            "У заявки №" + request.getId() + " нет регистрационного номера. Сначала примите заявку."
                    );
                }

                for (RequestRegistrationNumber number : numbers) {
                    createCertificate(sheet, styles, request, number, rowIndex);
                    rowIndex += BLOCK_HEIGHT;

                    if ((rowIndex / BLOCK_HEIGHT) % 2 == 0) {
                        sheet.setRowBreak(rowIndex - GAP_HEIGHT - 1);
                    }
                }
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при формировании справок для печати", e);
        }
    }

    public List<CertificatePrintPreviewDto> buildPrintPreview(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            throw new IllegalArgumentException("Не выбраны заявки для предпросмотра справок");
        }

        List<Request> requests = requestRepository.findAllById(requestIds);

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Заявки не найдены");
        }

        Map<Long, List<RequestRegistrationNumber>> numbersByRequestId = loadRegistrationNumbers(requests);

        List<CertificatePrintPreviewDto> result = new ArrayList<>();

        for (Request request : requests) {
            List<RequestRegistrationNumber> numbers = numbersByRequestId.getOrDefault(request.getId(), List.of());

            if (numbers.isEmpty()) {
                throw new IllegalArgumentException(
                        "У заявки №" + request.getId() + " нет регистрационного номера. Сначала примите заявку."
                );
            }

            for (RequestRegistrationNumber number : numbers) {
                result.add(
                        CertificatePrintPreviewDto.builder()
                                .requestId(request.getId())
                                .registrationNumber(formatRegistrationNumber(number))
                                .issueDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                                .studentFullName(safe(request.getStudentFullName()))
                                .birthDate("{{studentBirthDate}}")
                                .course(request.getCourse() == null ? "{{course}}" : String.valueOf(request.getCourse()))
                                .facultyName(facultyName(request))
                                .direction("{{direction}}")
                                .profile("{{profile}}")
                                .groupName(safe(request.getGroupName()))
                                .educationForm("{{educationForm}}")
                                .educationBasis("{{educationBasis}}")
                                .educationLevel("{{educationLevel}}")
                                .studyPeriod("{{studyStartDate}} – {{studyEndDate}}")
                                .purpose(safe(request.getPurpose()))
                                .enrollmentOrder("{{enrollmentOrder}}")
                                .deanName("{{deanName}}")
                                .secretaryName("{{secretaryName}}")
                                .build()
                );
            }
        }

        return result;
    }

    private Map<Long, List<RequestRegistrationNumber>> loadRegistrationNumbers(List<Request> requests) {
        List<Long> requestIds = requests.stream()
                .map(Request::getId)
                .toList();

        Map<Long, List<RequestRegistrationNumber>> result = new LinkedHashMap<>();

        for (Long requestId : requestIds) {
            result.put(requestId, new ArrayList<>());
        }

        List<RequestRegistrationNumber> numbers =
                requestRegistrationNumberRepository.findByRequestIdInOrderByRequestIdAscRegistrationNumberAsc(requestIds);

        for (RequestRegistrationNumber number : numbers) {
            result.computeIfAbsent(number.getRequestId(), key -> new ArrayList<>()).add(number);
        }

        for (Request request : requests) {
            List<RequestRegistrationNumber> requestNumbers = result.getOrDefault(request.getId(), new ArrayList<>());

            if (requestNumbers.isEmpty()
                    && request.getRegistrationNumber() != null
                    && request.getRegistrationYear() != null
                    && request.getFacultyId() != null) {
                requestNumbers.add(
                        RequestRegistrationNumber.builder()
                                .requestId(request.getId())
                                .facultyId(request.getFacultyId())
                                .registrationNumber(request.getRegistrationNumber())
                                .registrationYear(request.getRegistrationYear())
                                .createdAt(request.getRegisteredAt())
                                .build()
                );

                result.put(request.getId(), requestNumbers);
            }
        }

        return result;
    }

    private void setupSheet(HSSFSheet sheet) {
        sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.getPrintSetup().setLandscape(false);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);

        sheet.setMargin(Sheet.TopMargin, 0.35);
        sheet.setMargin(Sheet.BottomMargin, 0.35);
        sheet.setMargin(Sheet.LeftMargin, 0.35);
        sheet.setMargin(Sheet.RightMargin, 0.35);

        int[] widths = { 2400, 2400, 2400, 2400, 2400, 2400, 2400, 2400 };

        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createCertificate(
            HSSFSheet sheet,
            Styles styles,
            Request request,
            RequestRegistrationNumber number,
            int startRow
    ) {
        StudentData studentData = loadStudentDataPlaceholder(request);
        String registrationNumber = formatRegistrationNumber(number);
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        for (int i = startRow; i < startRow + CERTIFICATE_HEIGHT; i++) {
            HSSFRow row = sheet.createRow(i);
            row.setHeightInPoints(18);
            for (int c = 0; c < 8; c++) {
                HSSFCell cell = row.createCell(c);
                cell.setCellStyle(styles.defaultStyle);
            }
        }

        merge(sheet, startRow, startRow, 0, 7);
        setCell(sheet, startRow, 0, "СПРАВКА", styles.titleStyle);

        merge(sheet, startRow + 1, startRow + 1, 0, 7);
        setCell(sheet, startRow + 1, 0, "№ " + registrationNumber + " от " + currentDate, styles.centerStyle);

        merge(sheet, startRow + 3, startRow + 3, 0, 7);
        setCell(sheet, startRow + 3, 0, "Дана " + studentData.fullName, styles.textStyle);

        merge(sheet, startRow + 4, startRow + 4, 0, 7);
        setCell(sheet, startRow + 4, 0, "в том, что он(а) действительно является обучающимся", styles.textStyle);

        merge(sheet, startRow + 5, startRow + 5, 0, 7);
        setCell(sheet, startRow + 5, 0, "ФГБОУ ВО «Благовещенский государственный педагогический университет»", styles.textStyle);

        merge(sheet, startRow + 7, startRow + 7, 0, 3);
        setCell(sheet, startRow + 7, 0, "Факультет:", styles.labelStyle);
        merge(sheet, startRow + 7, startRow + 7, 4, 7);
        setCell(sheet, startRow + 7, 4, facultyName(request), styles.valueStyle);

        merge(sheet, startRow + 8, startRow + 8, 0, 3);
        setCell(sheet, startRow + 8, 0, "Курс, группа:", styles.labelStyle);
        merge(sheet, startRow + 8, startRow + 8, 4, 7);
        setCell(sheet, startRow + 8, 4, buildCourseGroup(request), styles.valueStyle);

        merge(sheet, startRow + 9, startRow + 9, 0, 3);
        setCell(sheet, startRow + 9, 0, "Форма обучения:", styles.labelStyle);
        merge(sheet, startRow + 9, startRow + 9, 4, 7);
        setCell(sheet, startRow + 9, 4, studentData.educationForm, styles.valueStyle);

        merge(sheet, startRow + 10, startRow + 10, 0, 3);
        setCell(sheet, startRow + 10, 0, "Уровень образования:", styles.labelStyle);
        merge(sheet, startRow + 10, startRow + 10, 4, 7);
        setCell(sheet, startRow + 10, 4, studentData.educationLevel, styles.valueStyle);

        merge(sheet, startRow + 11, startRow + 11, 0, 3);
        setCell(sheet, startRow + 11, 0, "Основание зачисления:", styles.labelStyle);
        merge(sheet, startRow + 11, startRow + 11, 4, 7);
        setCell(sheet, startRow + 11, 4, studentData.enrollmentOrder, styles.valueStyle);

        merge(sheet, startRow + 12, startRow + 12, 0, 3);
        setCell(sheet, startRow + 12, 0, "Период обучения:", styles.labelStyle);
        merge(sheet, startRow + 12, startRow + 12, 4, 7);
        setCell(sheet, startRow + 12, 4, studentData.studyPeriod, styles.valueStyle);

        merge(sheet, startRow + 14, startRow + 14, 0, 7);
        setCell(sheet, startRow + 14, 0, buildStipendLine(request), styles.textStyle);

        merge(sheet, startRow + 16, startRow + 16, 0, 7);
        setCell(sheet, startRow + 16, 0, "Справка выдана для предъявления " + safe(request.getPurpose()) + ".", styles.textStyle);

        merge(sheet, startRow + 20, startRow + 20, 0, 3);
        setCell(sheet, startRow + 20, 0, "Декан факультета", styles.textStyle);

        merge(sheet, startRow + 20, startRow + 20, 5, 7);
        setCell(sheet, startRow + 20, 5, "______________", styles.centerStyle);

        merge(sheet, startRow + 22, startRow + 22, 0, 3);
        setCell(sheet, startRow + 22, 0, "Исполнитель", styles.textStyle);

        merge(sheet, startRow + 22, startRow + 22, 5, 7);
        setCell(sheet, startRow + 22, 5, "______________", styles.centerStyle);

        merge(sheet, startRow + 25, startRow + 25, 0, 7);
        setCell(sheet, startRow + 25, 0, "Данные обучающегося будут подгружаться из 1С:Университет при интеграции.", styles.noteStyle);

        applyOuterBorder(sheet, startRow, startRow + CERTIFICATE_HEIGHT - 2, 0, 7, styles.borderStyle);
    }

    private StudentData loadStudentDataPlaceholder(Request request) {
        return StudentData.builder()
                .fullName(safe(request.getStudentFullName()))
                .birthDate("{{studentBirthDate}}")
                .educationForm("{{educationForm}}")
                .educationLevel("{{educationLevel}}")
                .enrollmentOrder("{{enrollmentOrder}}")
                .studyPeriod("{{studyStartDate}} – {{studyEndDate}}")
                .recordBookNumber("{{recordBookNumber}}")
                .build();
    }

    private String buildStipendLine(Request request) {
        if ("WITH_STIPEND".equals(request.getCertificateType())) {
            if (request.getPeriodFrom() != null && request.getPeriodTo() != null) {
                return "Справка содержит сведения о стипендии за период: "
                        + formatDate(request.getPeriodFrom())
                        + " – "
                        + formatDate(request.getPeriodTo())
                        + ".";
            }

            return "Справка содержит сведения о стипендии.";
        }

        return "Справка выдана без отметки о стипендии.";
    }

    private String buildCourseGroup(Request request) {
        String course = request.getCourse() == null ? "" : request.getCourse() + " курс";
        String group = safe(request.getGroupName());

        if (!course.isBlank() && !group.equals("—")) {
            return course + ", " + group;
        }

        if (!course.isBlank()) {
            return course;
        }

        return group;
    }

    private String facultyName(Request request) {
        if (request.getFacultyId() == null) {
            return safe(request.getFacultyName());
        }

        return facultyRepository.findById(request.getFacultyId())
                .map(Faculty::getName)
                .orElse(safe(request.getFacultyName()));
    }

    private String formatRegistrationNumber(RequestRegistrationNumber number) {
        String facultyCode = facultyRepository.findById(number.getFacultyId())
                .map(Faculty::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.matches("\\d+") ? String.format("%02d", Integer.parseInt(code)) : code)
                .orElse(String.format("%02d", number.getFacultyId()));

        String regNumber = String.format("%04d", number.getRegistrationNumber());
        String year = String.valueOf(number.getRegistrationYear()).substring(2);

        return facultyCode + "-" + regNumber + "/" + year;
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "—";
        }

        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private void setCell(HSSFSheet sheet, int rowIndex, int colIndex, String value, HSSFCellStyle style) {
        HSSFRow row = getOrCreateRow(sheet, rowIndex);
        HSSFCell cell = row.getCell(colIndex);

        if (cell == null) {
            cell = row.createCell(colIndex);
        }

        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void merge(HSSFSheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    private HSSFRow getOrCreateRow(HSSFSheet sheet, int rowIndex) {
        HSSFRow row = sheet.getRow(rowIndex);

        if (row == null) {
            row = sheet.createRow(rowIndex);
        }

        return row;
    }

    private void applyOuterBorder(
            HSSFSheet sheet,
            int firstRow,
            int lastRow,
            int firstCol,
            int lastCol,
            HSSFCellStyle borderStyle
    ) {
        for (int r = firstRow; r <= lastRow; r++) {
            HSSFRow row = getOrCreateRow(sheet, r);

            for (int c = firstCol; c <= lastCol; c++) {
                HSSFCell cell = row.getCell(c);

                if (cell == null) {
                    cell = row.createCell(c);
                }

                cell.setCellStyle(borderStyle);
            }
        }
    }

    private Styles createStyles(HSSFWorkbook workbook) {
        HSSFFont defaultFont = workbook.createFont();
        defaultFont.setFontName("Times New Roman");
        defaultFont.setFontHeightInPoints((short) 10);

        HSSFFont titleFont = workbook.createFont();
        titleFont.setFontName("Times New Roman");
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setBold(true);

        HSSFFont boldFont = workbook.createFont();
        boldFont.setFontName("Times New Roman");
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setBold(true);

        HSSFFont noteFont = workbook.createFont();
        noteFont.setFontName("Times New Roman");
        noteFont.setFontHeightInPoints((short) 8);
        noteFont.setItalic(true);

        HSSFCellStyle defaultStyle = baseStyle(workbook, defaultFont, HorizontalAlignment.LEFT);
        HSSFCellStyle titleStyle = baseStyle(workbook, titleFont, HorizontalAlignment.CENTER);
        HSSFCellStyle centerStyle = baseStyle(workbook, defaultFont, HorizontalAlignment.CENTER);
        HSSFCellStyle textStyle = baseStyle(workbook, defaultFont, HorizontalAlignment.LEFT);
        HSSFCellStyle labelStyle = baseStyle(workbook, boldFont, HorizontalAlignment.LEFT);
        HSSFCellStyle valueStyle = baseStyle(workbook, defaultFont, HorizontalAlignment.LEFT);
        HSSFCellStyle noteStyle = baseStyle(workbook, noteFont, HorizontalAlignment.CENTER);
        HSSFCellStyle borderStyle = baseStyle(workbook, defaultFont, HorizontalAlignment.LEFT);

        return Styles.builder()
                .defaultStyle(defaultStyle)
                .titleStyle(titleStyle)
                .centerStyle(centerStyle)
                .textStyle(textStyle)
                .labelStyle(labelStyle)
                .valueStyle(valueStyle)
                .noteStyle(noteStyle)
                .borderStyle(borderStyle)
                .build();
    }

    private HSSFCellStyle baseStyle(HSSFWorkbook workbook, HSSFFont font, HorizontalAlignment alignment) {
        HSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        return style;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    @lombok.Builder
    private static class StudentData {
        private String fullName;
        private String birthDate;
        private String educationForm;
        private String educationLevel;
        private String enrollmentOrder;
        private String studyPeriod;
        private String recordBookNumber;
    }

    @lombok.Builder
    private static class Styles {
        private HSSFCellStyle defaultStyle;
        private HSSFCellStyle titleStyle;
        private HSSFCellStyle centerStyle;
        private HSSFCellStyle textStyle;
        private HSSFCellStyle labelStyle;
        private HSSFCellStyle valueStyle;
        private HSSFCellStyle noteStyle;
        private HSSFCellStyle borderStyle;
    }
}