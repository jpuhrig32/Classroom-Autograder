package org.juhrig.classroom.autograder.output;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ExcelWriter {

    private String filename;
    private XSSFWorkbook parentWorkbook;
    private SXSSFWorkbook workbook;
    private SXSSFSheet currentSheet;
    private int rowCount;
    private ReentrantLock lock = new ReentrantLock();

    public XSSFCellStyle RED;
    public XSSFCellStyle GREEN;
    private static final Logger LOG = LoggerFactory.getLogger(ExcelWriter.class);
    private static final long IGNORE_NUMBERS_ABOVE = 1000000;

    public ExcelWriter(String outputFileName) throws IOException {
        filename = outputFileName;

        parentWorkbook = new XSSFWorkbook();
        XSSFColor colorRed = new XSSFColor(new java.awt.Color(255, 0, 0), new DefaultIndexedColorMap());
        RED = parentWorkbook.createCellStyle();
        RED.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        RED.setFillForegroundColor(colorRed);
        XSSFColor colorGreen = new XSSFColor(new java.awt.Color(0, 255, 0), new DefaultIndexedColorMap());
        GREEN = parentWorkbook.createCellStyle();;
        GREEN.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        GREEN.setFillForegroundColor(colorGreen);

        workbook = new SXSSFWorkbook(parentWorkbook);
        currentSheet = workbook.createSheet("Grade Output");
        rowCount = 0;

    }

    public void writeHeaderRow(List<String> headerItems){
        try {
            lock.lock();
            int currentCell = 0;
            SXSSFRow row = currentSheet.createRow(rowCount++);
            Iterator headerIterator = headerItems.iterator();
            while (headerIterator.hasNext()) {
                SXSSFCell cell = row.createCell(currentCell++, CellType.STRING);
                cell.setCellValue((String) headerIterator.next());
            }
        }
        finally {
            lock.unlock();
        }
    }
    public void writeRow(List<String> rowValues){
        writeRow(rowValues, null);
    }
    public void writeRow(List<String> rowValues, CellStyle cellStyle){
        try {
            lock.lock();
            int currentCell = 0;
            SXSSFRow row = currentSheet.createRow(rowCount++);
            Iterator rowIterator = rowValues.iterator();
            while (rowIterator.hasNext()) {
                String cellStringValue = (String) rowIterator.next();
                SXSSFCell cell = row.createCell(currentCell++);
                if(isNumericValue(cellStringValue)){
                    double cellValue = Double.parseDouble(cellStringValue);
                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellValue(cellValue);
                }
                else {
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(cellStringValue);
                }
                if(cellStyle != null){
                    cell.setCellStyle(cellStyle);
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void insertEmptyRow(){
        try {
            lock.lock();
            SXSSFRow row = currentSheet.createRow(rowCount++);
        }
        finally {
            lock.unlock();
        }
    }


    private boolean isNumericValue(String cellValue){
        try{
            Double value = Double.parseDouble(cellValue);
            if(value > IGNORE_NUMBERS_ABOVE){
                return false;
            }
            return true;
        }
        catch (NumberFormatException e){
            return false;
        }
    }

    public void close(){
        try {
            Path outputPath = Paths.get(filename);
            File outputParent = outputPath.getParent().toFile();
            if(!outputParent.exists()){
                outputParent.mkdirs();
            }
            FileOutputStream outFile = new FileOutputStream(filename);
            workbook.write(outFile);
            outFile.close();
            workbook.close();

        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }
        workbook.dispose();
    }


}
