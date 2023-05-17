package org.script.upload;

import com.beust.ah.A;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class Excel {
    public static final String PROCESSED = "Processed";
    private String filename;
    private boolean isNewExcel; //set true if file is xslt
    private int sheetIndex;
    private Map<Integer, String> headerRow;
    private Workbook workbook;
    private Sheet sheet;

    public Excel(String filename, boolean isNewExcel, int sheetIndex) {
        this.filename = filename;
        this.isNewExcel = isNewExcel;
        this.sheetIndex = sheetIndex;
    }

    public Sheet open() {
        this.sheet = getSheet(sheetIndex);
        this.headerRow = getHeaderRow(sheet);
        return sheet;
    }

    public List<Map<String, String>> readExcel() throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 1; i < sheet.getLastRowNum(); i++) {
            Map<String, String> rowContent = getRowContent(sheet.getRow(i), headerRow);
            if (rowContent.values().stream().anyMatch(StringUtils::isNotEmpty)) {
                rowContent.put("line", String.valueOf(i));
                result.add(rowContent);
            }
        }
        return result;
    }

    private Map<Integer, String> getHeaderRow(Sheet sheet) {
        Map<Integer, String> rowData = new HashMap();
        Row row = sheet.getRow(0);
        for (int rowIdx = row.getFirstCellNum(); rowIdx <= row.getLastCellNum(); rowIdx++) {
            Cell cell = row.getCell(rowIdx);
            String value = new DataFormatter().formatCellValue(cell);
            if (StringUtils.isNotEmpty(value)) {
                rowData.put(rowIdx, value);
            }
        }
        return rowData;
    }

    private Map<String, String> getRowContent(Row row, Map<Integer, String> headerRow) {
        Map<String, String> rowContent = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (int cellIndex = row.getFirstCellNum(); cellIndex <= row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            String cellValue = new DataFormatter().formatCellValue(cell);
            if (headerRow.containsKey(cellIndex)) {
                //Header row has value in this column, so expected to find value in cell
                if (StringUtils.isEmpty(cellValue)) {
                    errors.add("Attention! Expected to find cell not empty. Column - " + (cellIndex + 1) + ". Header - '" + headerRow.get(cellIndex) + "'");
                }
                rowContent.put(headerRow.get(cellIndex), cellValue);
            } else {
                //If for some reason cell is not empty, but has no header - add info about this issue
                if (StringUtils.isNotEmpty(cellValue)) {
                    errors.add("Attention! Expected to find cell EMPTY, but found " + cellValue + "; Column - " + (cellIndex + 1));
                    rowContent.put("Unknown column #" + (cellIndex + 1), cellValue);
                }
            }
        }
        //If whole row is empty - that's ok, not needed to print errors
        if (!rowContent.values().stream().allMatch(String::isEmpty) && !errors.isEmpty()) {
            System.out.println("----Errors found in line " + row.getRowNum() + ": -----");
            errors.forEach(System.out::println);
            System.out.println("----End of Errors in line " + row.getRowNum() + ": -----");
        }
        return rowContent;
    }

    public void addFlagThatLineProcessed(int rowIdx) throws
            InvalidFormatException, IOException {
        Optional<Map.Entry<Integer, String>> processed = headerRow.entrySet().stream().filter(e -> e.getValue().equals(PROCESSED)).findFirst();
        int columnIndexForProcess = processed.isPresent() ? processed.get().getKey() : putStrToCell(sheet, 0, sheet.getRow(0).getLastCellNum() + 1, PROCESSED);
        putStrToCell(sheet, rowIdx, columnIndexForProcess, "Y");
    }

    private int putStrToCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (Objects.isNull(cell)) {
            cell = row.createCell(colIdx);
        }
        cell.setCellValue(value);

        return colIdx;
    }


    public void save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Sheet getSheet(int sheetIndex) {
        try (FileInputStream fis = new FileInputStream(filename)) {
            workbook = isNewExcel ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis);
            sheet = workbook.getSheetAt(sheetIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sheet;
    }
}
