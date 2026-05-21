package com.xingzhewk.service.impl;

import com.xingzhewk.common.exception.BusinessException;
import com.xingzhewk.mapper.BillMapper;
import com.xingzhewk.service.BillExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillExportServiceImpl implements BillExportService {

    private static final int MAX_EXPORT_ROWS = 50000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {"账单时间", "类型", "分类", "金额", "备注", "创建时间"};

    private final BillMapper billMapper;

    @Override
    public void exportBills(Long userId, String format, String month, String type,
                            Long categoryId, String keyword, String startDate, String endDate,
                            HttpServletResponse response) {
        List<Map<String, Object>> rows = billMapper.selectForExport(
                userId, month, type, categoryId, keyword, startDate, endDate);

        if (rows.size() > MAX_EXPORT_ROWS) {
            throw new BusinessException(400, "导出数据超过 50000 行限制，请缩小筛选范围");
        }

        String filename = buildFilename(month, startDate, endDate, format);

        if ("xlsx".equalsIgnoreCase(format)) {
            writeExcel(rows, filename, response);
        } else {
            writeCsv(rows, filename, response);
        }
    }

    private void writeCsv(List<Map<String, Object>> rows, String filename, HttpServletResponse response) {
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            // UTF-8 BOM
            writer.write('﻿');
            // Header
            writer.write(String.join(",", HEADERS));
            writer.newLine();
            // Data
            for (Map<String, Object> row : rows) {
                String[] cells = new String[HEADERS.length];
                cells[0] = formatTime(row.get("bill_time"));
                cells[1] = formatType(row.get("type"));
                cells[2] = escapeCsv(String.valueOf(row.get("category_name")));
                cells[3] = formatAmount(row.get("amount"));
                cells[4] = escapeCsv(String.valueOf(row.get("remark")));
                cells[5] = formatTime(row.get("created_at"));
                writer.write(String.join(",", cells));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            throw new BusinessException(500, "导出 CSV 失败: " + e.getMessage());
        }
    }

    private void writeExcel(List<Map<String, Object>> rows, String filename, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("账单");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Amount style
            CellStyle amountStyle = wb.createCellStyle();
            amountStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int r = 0; r < rows.size(); r++) {
                Map<String, Object> row = rows.get(r);
                Row dataRow = sheet.createRow(r + 1);

                Cell c0 = dataRow.createCell(0);
                c0.setCellValue(formatTime(row.get("bill_time")));
                Cell c1 = dataRow.createCell(1);
                c1.setCellValue(formatType(row.get("type")));
                Cell c2 = dataRow.createCell(2);
                c2.setCellValue(String.valueOf(row.get("category_name")));
                Cell c3 = dataRow.createCell(3);
                c3.setCellValue(toDouble(row.get("amount")));
                c3.setCellStyle(amountStyle);
                Cell c4 = dataRow.createCell(4);
                c4.setCellValue(String.valueOf(row.get("remark")));
                Cell c5 = dataRow.createCell(5);
                c5.setCellValue(formatTime(row.get("created_at")));
            }

            wb.write(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException(500, "导出 Excel 失败: " + e.getMessage());
        }
    }

    private String buildFilename(String month, String startDate, String endDate, String format) {
        String base;
        if (StringUtils.hasText(month)) {
            base = "bills_" + month;
        } else if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            base = "bills_" + startDate + "_to_" + endDate;
        } else {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            base = "bills_all_" + today;
        }
        return base + "." + format;
    }

    private String formatTime(Object time) {
        if (time == null) return "";
        if (time instanceof LocalDateTime) {
            return ((LocalDateTime) time).format(TIME_FMT);
        }
        return time.toString();
    }

    private String formatType(Object type) {
        if ("EXPENSE".equals(type)) return "支出";
        if ("INCOME".equals(type)) return "收入";
        return type != null ? type.toString() : "";
    }

    private String formatAmount(Object amount) {
        if (amount == null) return "0.00";
        return ((BigDecimal) amount).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private double toDouble(Object amount) {
        if (amount == null) return 0.0;
        return ((BigDecimal) amount).doubleValue();
    }

    private String escapeCsv(String value) {
        if (value == null || "null".equals(value)) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
