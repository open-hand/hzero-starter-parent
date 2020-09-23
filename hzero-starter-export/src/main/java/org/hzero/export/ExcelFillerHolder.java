package org.hzero.export;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hzero.export.filler.ExcelFiller;
import org.hzero.export.filler.MultiSheetFiller;
import org.hzero.export.filler.SingleSheetFiller;
import org.hzero.export.filler.SqlSheetFiller;
import org.hzero.export.filler.v2.MultiSheetFillerV2;
import org.hzero.export.filler.v2.SingleSheetFillerV2;
import org.hzero.export.vo.ExportColumn;

import java.util.Objects;

/**
 * Filler holder
 *
 * @author bojiangzhou 2018/08/08
 */
public class ExcelFillerHolder {

    /**
     * 获取ExcelFiller
     */
    public static ExcelFiller getExcelFiller(String fillerType, ExportColumn exportColumn, SXSSFWorkbook workbook) {
        Class<?> clazz = Objects.requireNonNull(FillerType.of(fillerType, false)).clazz;
        try {
            return (ExcelFiller) clazz.getConstructor(ExportColumn.class, SXSSFWorkbook.class).newInstance(workbook, exportColumn);
        } catch (Exception e) {
            throw new RuntimeException("not found", e);
        }
    }

    public static IExcelFiller getExcelFillerV2(String fillerType, ExportColumn exportColumn) {
        Class<?> clazz = Objects.requireNonNull(FillerType.of(fillerType, true)).clazz;
        try {
            return (IExcelFiller) clazz.getConstructor(ExportColumn.class).newInstance(exportColumn);
        } catch (Exception e) {
            throw new RuntimeException("not found", e);
        }
    }

    public enum FillerType{
        SINGLE_SHEET("single-sheet", SingleSheetFiller.class),
        SINGLE_SHEET_V2("single-sheet", SingleSheetFillerV2.class),
        MULTI_SHEET("multi-sheet", MultiSheetFiller.class),
        MULTI_SHEET_V2("multi-sheet", MultiSheetFillerV2.class),
        SQL_SHEET("sql-sheet", SqlSheetFiller.class);
        private String fillerType;
        private Class<?> clazz;

        FillerType(String fillerType, Class<?> clazz) {
            this.fillerType = fillerType;
            this.clazz = clazz;
        }

        public static FillerType of(String fillerType, boolean ignoreDeprecated) {
            for (FillerType type : values()) {
                if (ignoreDeprecated
                        && !type.clazz.isAnnotationPresent(Deprecated.class)
                        && fillerType.equals(type.fillerType)) {
                    return type;
                } else if (!ignoreDeprecated && fillerType.equals(type.fillerType)){
                    return type;
                }
            }
            return null;
        }
    }

}
