package org.hzero.export.filler;

import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hzero.export.exporter.ExcelExporter;
import org.hzero.export.vo.ExportColumn;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * sql执行结果导出 Filler
 *
 * @author zhiying.dong@hand-china.com 2018/10/16 16:52
 */
@Deprecated
public class SqlSheetFiller extends ExcelFiller {
    public static final String FILLER_TYPE = "sql-sheet";

    public SqlSheetFiller() {}

    public SqlSheetFiller(ExportColumn root, SXSSFWorkbook workbook) {
        super(root, workbook);
    }

    @Override
    public String getFillerType() {
        return FILLER_TYPE;
    }

    @Override
    public void fillData(SXSSFWorkbook workbook, ExportColumn exportClass, List<?> data) {
        if (data == null) {
            return;
        }
        if (workbook.getNumberOfSheets() == 0) {
            createSheetAndTitle(workbook, exportClass);
        }
        for(int i = 0,len = data.size(); i < len; i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            sheet.setRandomAccessWindowSize(-1);
            fillData(sheet, exportClass.getChildren().get(i), (List<?>) data.get(i));
        }
    }

    private void fillData(SXSSFSheet sheet, ExportColumn exportClass, List<?> data) {
        // 处理每行数据
        for (int i = 0, len = data.size(); i < len; i++) {
            Assert.isTrue(sheet.getLastRowNum() < ExcelExporter.MAX_ROW, "export.too-many-data");
            SXSSFRow row = sheet.createRow(i+1);
            fillRow(row, (Map<?, ?>) data.get(i), exportClass.getChildren());
        }
    }

    /**
     * sheet 标题最长31位 多余的会被截掉，标题不能重复
     */
    @Override
    public void createSheetAndTitle(SXSSFWorkbook workbook, ExportColumn exportClass) {
        for(int i = 0,len = exportClass.getChildren().size(); i < len; i++) {
            SXSSFSheet sheet = workbook.createSheet("Sheet"+(i+1));
            sheet.setDefaultColumnWidth(20);
            createTitleRow(sheet, exportClass.getChildren().get(i));
        }
    }


    /**
     * 创建标题行
     */
    private void createTitleRow(SXSSFSheet sheet, ExportColumn titleColumns) {
        throw new UnsupportedOperationException("in development...");
//        SXSSFRow titleRow = sheet.createRow(0);
//        titleRow.setHeight((short) 350);
//        int colOffset = 0;
//        for (ExportColumn column : titleColumns.getChildren()) {
//            SXSSFCell cell = titleRow.createCell(colOffset);
//            // 值
//            fillCellValue(cell, column.getName(), "String","");
//            // 宽度
//            setCellWidth(sheet, "String", colOffset);
//            //样式
//            cell.setCellStyle(titleCellStyle);
//            colOffset++;
//        }
    }

    /**
     * 生成行数据
     */
    private void fillRow(SXSSFRow row, Map<?, ?> rowData, List<ExportColumn> columns) {
        throw new UnsupportedOperationException("in development...");
//        int cells = 0;
//        for (ExportColumn column : columns) {
//            String cellValue = null;
//            try {
//                cellValue = rowData.get(column.getName()).toString();
//            } catch (Exception ev) {
//                logger.error("get value error.");
//            }
//            SXSSFCell cell = row.createCell(cells);
//            fillCellValue(cell, cellValue, "String","");
//            cells++;
//        }
    }

}
