package org.hzero.export.filler;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hzero.export.annotation.ExcelSheet;
import org.hzero.export.exporter.ExcelExporter;
import org.hzero.export.vo.ExportColumn;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 单Sheet结构化导出：父类和子类在一个Sheet页，结构化显示数据
 *
 * @author bojiangzhou 2018/08/23
 */
@Deprecated
public class SingleSheetStructureFiller extends ExcelFiller {

    public static final String FILLER_TYPE = "single-sheet-structure";

    /**
     * 标题样式
     */
    private CellStyle childTitleCellStyle;

    private int currentSheetIndex;

    public SingleSheetStructureFiller() {}

    public SingleSheetStructureFiller(ExportColumn root, SXSSFWorkbook workbook) {
        super(root, workbook);
        setChildTitleCellStyle();
    }

    private int childCount = 0;

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
            // 如果子集超过1个，则每个父级数据对应的子集分别创建标题
            childCount = getCheckedChildren(exportClass).size();
        }

        fillData(exportClass, exportClass.getExcelSheet().colOffset(), data);
        currentSheetIndex++;
    }

    private void fillData(ExportColumn exportClass, int colOffset, List<?> data) {
        SXSSFSheet sheet = workbook.getSheetAt(currentSheetIndex);
        List<ExportColumn> checkedChildren = getCheckedChildren(exportClass);
        // 处理每行数据
        for (int i = 0, len = data.size(); i < len; i++) {
            Assert.isTrue(sheet.getLastRowNum() < ExcelExporter.MAX_ROW, "export.too-many-data");
            SXSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
            // 行数据
            fillRow(row, colOffset, exportClass.getExcelSheet().placeholder(), data.get(i), exportClass.getChildren());
            // 子列表
            for (ExportColumn child : checkedChildren) {
                List<Object> childData = getChildData(data.get(i), child);
                if (CollectionUtils.isNotEmpty(childData)) {
                    int childColOffset = child.getExcelSheet().colOffset() <= 0 ? 1 + colOffset : child.getExcelSheet().colOffset();
                    if (childCount > 1) {
                        createTitleRow(sheet, sheet.getLastRowNum() + 1, childColOffset, child.getExcelSheet().placeholder(), true, child.getChildren());
                    }
                    fillData(child, childColOffset, childData);
                }
            }
        }
    }

    /**
     * 首先创建父级标题，如果子集只有一个，则创建子标题
     */
    @Override
    public void createSheetAndTitle(SXSSFWorkbook workbook, ExportColumn exportClass) {
        // sheet
        SXSSFSheet sheet = workbook.createSheet(StringUtils.defaultIfBlank(exportClass.getTitle(), exportClass.getName()));
        sheet.setDefaultColumnWidth(20);
        int rowOffset = exportClass.getExcelSheet().rowOffset();
        int colOffset = exportClass.getExcelSheet().colOffset();
        // title
        createTitleRow(sheet, rowOffset, colOffset, null, false, exportClass.getChildren());

        if (childCount == 1) {
            ExportColumn child = exportClass.getChildren().stream()
                    .filter(ExportColumn::hasChildren)
                    .collect(Collectors.toList())
                    .get(0);
            ExcelSheet childSheet = child.getExcelSheet();
            int childRowOffset = childSheet.rowOffset() <= 0 ? 1 + rowOffset : childSheet.rowOffset();
            int childColOffset = childSheet.colOffset() <= 0 ? 1 + colOffset : childSheet.colOffset();
            createTitleRow(sheet, childRowOffset, childColOffset, childSheet.placeholder(), true, child.getChildren());
        }
    }

    /**
     * 创建标题行
     */
    private void createTitleRow(SXSSFSheet sheet, int rowOffset, int colOffset, String placeholder, boolean isChild, List<ExportColumn> columns) {
        SXSSFRow titleRow = sheet.createRow(rowOffset);
        int cellIndex;
        for (cellIndex = 0; cellIndex < colOffset; cellIndex++) {
            if (isChild) {
                fillCellValue(titleRow.createCell(cellIndex), placeholder, Collections.emptyList(), null, true);
            }
        }
        for (ExportColumn column : columns) {
            if (column.isChecked() && !column.getExcelColumn().child()) {
                SXSSFCell cell = titleRow.createCell(cellIndex);
                // 值
                fillCellValue(cell, column.getTitle(), Collections.emptyList(), column.getExcelColumn(), true);
                if (isChild) {
                    cell.setCellStyle(childTitleCellStyle);
                } else {
                    titleRow.setHeight((short) 350);
                    cell.setCellStyle(titleCellStyle);
                }
                // 宽度
                setCellWidth(sheet, column.getType(), cellIndex, column.getExcelColumn().width());

                cellIndex++;
            }
        }
    }

    /**
     * 生成行数据
     */
    private void fillRow(SXSSFRow row, int colOffset, String placeholder, Object rowData, List<ExportColumn> columns) {
        int cellIndex;
        for (cellIndex = 0;cellIndex < colOffset;cellIndex++) {
            fillCellValue(row.createCell(cellIndex), placeholder, null,  null, false);
        }
        for (ExportColumn column : columns) {
            if (column.isChecked() && !column.hasChildren()) {
                String cellValue = null;
                try {
                    cellValue = BeanUtils.getProperty(rowData, column.getName());
                } catch (Exception ev) {
                    logger.error("get value error.", ev);
                }
                fillCellValue(row.createCell(cellIndex++), cellValue, rowData, column.getExcelColumn(), false);
            }
        }
    }

    private List<ExportColumn> getCheckedChildren(ExportColumn root) {
        if (root != null && CollectionUtils.isNotEmpty(root.getChildren())) {
            return root.getChildren().stream().filter(column -> column.isChecked() && column.hasChildren()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Object> getChildData(Object data, ExportColumn child) {
        String getter = "get" + child.getName();
        Method[] methods = data.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(getter)) {
                try {
                    method.setAccessible(true);
                    return (List<Object>) method.invoke(data);
                } catch (Exception e) {
                    logger.error("get child data error.", e);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 设置子标题单元格样式
     */
    protected void setChildTitleCellStyle() {
        Font font = workbook.createFont();
        font.setColor(Font.COLOR_NORMAL);

        childTitleCellStyle = workbook.createCellStyle();
        childTitleCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        childTitleCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        childTitleCellStyle.setFont(font);
        childTitleCellStyle.setBorderTop(BorderStyle.THIN);
        childTitleCellStyle.setBorderBottom(BorderStyle.THIN);
    }

}
