package org.hzero.export.filler;

import io.choerodon.core.exception.CommonException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFDrawing;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.hzero.core.util.Pair;
import org.hzero.core.util.SecurityUtils;
import org.hzero.export.annotation.CommentProperty;
import org.hzero.export.annotation.EmptyColor;
import org.hzero.export.annotation.EmptyComment;
import org.hzero.export.annotation.EmptyFont;
import org.hzero.export.annotation.ExcelColumn;
import org.hzero.export.exporter.ExcelExporter;
import org.hzero.export.render.ValueRenderer;
import org.hzero.export.util.ShardUtils;
import org.hzero.export.vo.ExportColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel 数据填充器
 *
 * @author bojiangzhou 2018/07/27
 */
@Deprecated
public abstract class ExcelFiller {

    private Map<Class<? extends ValueRenderer>, ValueRenderer> renderers = new ConcurrentHashMap<>();
    private Map<ExcelColumn, CellStyle> styleMap = new ConcurrentHashMap<>();
    private Map<ExcelColumn, Font> fontMap = new ConcurrentHashMap<>();
    private Map<ExcelColumn, Comment> commentMap = new ConcurrentHashMap<>();
    private volatile DataFormat dataFormat;

    private ZoneId zoneId = ZoneId.systemDefault();

    protected Logger logger = LoggerFactory.getLogger(ExcelFiller.class);

    private ExportColumn root;

    /**
     * 工作铺
     */
    protected SXSSFWorkbook workbook;

    /**
     * 标题样式
     */
    protected CellStyle titleCellStyle;

    /**
     * 单个sheet页的最大记录数
     */
    protected Integer singleSheetMaxRow = ExcelExporter.MAX_ROW;
    private int pageIndex;

    public ExcelFiller() {
    }

    public ExcelFiller(ExportColumn root, SXSSFWorkbook workbook) {
        this.root = root;
        this.setWorkbook(workbook);
    }

    public void configure(int singleSheetMaxRow) {
        this.singleSheetMaxRow = singleSheetMaxRow;
    }


    /**
     * @return 填充器类型名称
     */
    public abstract String getFillerType();

    /**
     * 创建标题
     *
     * @param workbook 工作簿
     * @param root     导出列
     */
    public abstract void createSheetAndTitle(SXSSFWorkbook workbook, ExportColumn root);

    /**
     * 填充数据, 根据数据量划分成多个sheet
     *
     * @param workbook 工作簿
     * @param root     导出列
     * @param data     数据
     * @param paging   是否为分页处理
     */
    public void fillDataWithShard(SXSSFWorkbook workbook, ExportColumn root, List<?> data, boolean paging) {
        if (paging) {
            String title = root.getTitle();
            root.setTitle(root.getTitle() + "-" + (++pageIndex));
            createSheetAndTitle(workbook, root);
            fillData(workbook, root, data);
            root.setTitle(title);
            return;
        }
        if (singleSheetMaxRow != null) {
            int sum = data.size();
            Pair<Integer, Integer> pair = ShardUtils.shard(sum, singleSheetMaxRow);
            int shardNum = pair.getFirst();
            int lastNum = pair.getSecond();
            List<List<?>> shardData = ShardUtils.prepareShardData(data, shardNum, singleSheetMaxRow, lastNum);
            String title = root.getTitle();
            if (shardNum == 1) {
                root.setTitle(title);
                createSheetAndTitle(workbook, root);
                fillData(workbook, root, shardData.get(0));
            } else {
                for (int i = 0; i < shardNum; i++) {
                    root.setTitle(title + "-" + (i + 1));
                    createSheetAndTitle(workbook, root);
                    fillData(workbook, root, shardData.get(i));
                }
            }
            root.setTitle(title);
        } else {
            createSheetAndTitle(workbook, root);
            fillData(workbook, root, data);
        }
    }

    /**
     * 填充数据
     *
     * @param workbook 工作簿
     * @param root     导出列
     * @param data     数据
     */
    protected abstract void fillData(SXSSFWorkbook workbook, ExportColumn root, List<?> data);

    /**
     * 填充单元格
     *
     * @param cell      SXSSFCell
     * @param value     数据
     * @param rowData   行数据
     * @param excelColumn 行数据配置属性
     */
    protected void fillCellValue(SXSSFCell cell, Object value, Object rowData, ExcelColumn excelColumn, boolean isTitle) {

        List<Class<? extends ValueRenderer>> renderers = isTitle ? null : (excelColumn == null ? null : Arrays.asList(excelColumn.renderers()));
        Class<? extends org.hzero.export.annotation.Comment> comment = excelColumn == null ? null : excelColumn.comment();

        value = this.doRender(value, rowData, renderers);
        if (value == null) {
            cell.setCellType(CellType.BLANK);
            return;
        }
        Class<?> type = value.getClass();
        if (value instanceof Number) {
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellType(CellType.BOOLEAN);
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue((Date) value);
        } else if (value instanceof LocalDate) {
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(Date.from(((LocalDate) value).atStartOfDay(zoneId).toInstant()));
        } else if (value instanceof LocalDateTime) {
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(Date.from(((LocalDateTime) value).atZone(zoneId).toInstant()));
        } else if (value instanceof String) {
            cell.setCellType(CellType.STRING);
            // 防止 csv 注入
            value = SecurityUtils.preventCsvInjection((String) value);
            cell.setCellValue(String.valueOf(value));
        } else {
            this.logger.warn("can not process type [{}] in excel export", type);
            cell.setCellType(CellType.STRING);
            value = SecurityUtils.preventCsvInjection(String.valueOf(value));
            cell.setCellValue(String.valueOf(value));
        }

        CellStyle s = getCellStyle(excelColumn, isTitle);
        //标题无需添加注释
        if (!isTitle && comment != null) {
            setComment(cell, comment, excelColumn);
        }
        cell.setCellStyle(s);

    }

    private CellStyle getCellStyle(ExcelColumn excelColumn, boolean isTitle) {
        //标题无需缓存
        if (isTitle) {
            return createCellStyle(excelColumn, true);
        }
        return styleMap.computeIfAbsent(excelColumn, column -> {
            CellStyle columnCellStyle = createCellStyle(excelColumn, false);
            String pattern = excelColumn == null ? null : excelColumn.pattern();
            if (StringUtils.isNotBlank(pattern)) {
                //lazy init
                if (dataFormat == null) {
                    dataFormat = this.workbook.createDataFormat();
                }
                columnCellStyle.setDataFormat(dataFormat.getFormat(pattern));
            }
            return columnCellStyle;
        });
    }

    private CellStyle createCellStyle(ExcelColumn excelColumn, boolean isTitle) {
        CellStyle columnCellStyle = this.workbook.createCellStyle();
        Class<? extends org.hzero.export.annotation.Font> font =
                excelColumn == null ? null : (isTitle ? excelColumn.titleFont() : excelColumn.font());
        Class<? extends org.hzero.export.annotation.Color> foregroundColor =
                excelColumn == null ? null : (isTitle ? excelColumn.titleForegroundColor() : excelColumn.foregroundColor());
        Class<? extends org.hzero.export.annotation.Color> backgroundColor =
                excelColumn == null ? null : (isTitle ? excelColumn.titleBackgroundColor() : excelColumn.backgroundColor());
        if (font != null) {
            setFont(columnCellStyle, font, excelColumn);
        }
        if (foregroundColor != null) {
            setForegroundColor(columnCellStyle, foregroundColor);
        }
        if (backgroundColor != null) {
            setBackgroundColor(columnCellStyle, backgroundColor);
        }
        return columnCellStyle;
    }

    private void setFont(CellStyle style, Class<? extends org.hzero.export.annotation.Font> fontClass, ExcelColumn excelColumn) {
        org.hzero.export.annotation.Font font = getFontInstance(fontClass);
        if (font instanceof EmptyFont) {
            return;
        }
        style.setFont(font.getFont(fontMap.computeIfAbsent(excelColumn, column -> this.workbook.createFont())));
    }

    private org.hzero.export.annotation.Font getFontInstance(Class<? extends org.hzero.export.annotation.Font> fontClass) {
        try {
            return fontClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new CommonException("org.hzero.export.annotation.Font class newInstance() failed", e);
        }
    }

    private void setForegroundColor(CellStyle style, Class<? extends org.hzero.export.annotation.Color> foregroundColorClass) {
        org.hzero.export.annotation.Color foregroundColor = getForegroundColorInstance(foregroundColorClass);
        if (foregroundColor instanceof EmptyColor) {
            return;
        }
        IndexedColors colors = foregroundColor.getColor();
        FillPatternType fillPattern = foregroundColor.getFillPattern();
        style.setFillPattern(fillPattern);
        style.setFillForegroundColor(colors.getIndex());
    }

    private org.hzero.export.annotation.Color getForegroundColorInstance(Class<? extends org.hzero.export.annotation.Color> foregroundColorClass) {
        try {
            return foregroundColorClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new CommonException("org.hzero.export.annotation.ForegroundColor class newInstance() failed", e);
        }
    }

    private void setBackgroundColor(CellStyle style, Class<? extends org.hzero.export.annotation.Color> backgroundColorClass) {
        org.hzero.export.annotation.Color foregroundColor = getBackgroundColorInstance(backgroundColorClass);
        if (foregroundColor instanceof EmptyColor) {
            return;
        }
        IndexedColors colors = foregroundColor.getColor();
        FillPatternType fillPattern = foregroundColor.getFillPattern();
        style.setFillPattern(fillPattern);
        style.setFillBackgroundColor(colors.getIndex());
    }

    private org.hzero.export.annotation.Color getBackgroundColorInstance(Class<? extends org.hzero.export.annotation.Color> backgroundColorClass) {
        try {
            return backgroundColorClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new CommonException("org.hzero.export.annotation.ForegroundColor class newInstance() failed", e);
        }
    }

    private void setComment(SXSSFCell cell, Class<? extends org.hzero.export.annotation.Comment> commentClass, ExcelColumn excelColumn) {
        org.hzero.export.annotation.Comment comment = getCommentInstance(commentClass);
        if (comment instanceof EmptyComment) {
            return;
        }
        CommentProperty commentProperty = comment.getComment();
        if (cell.getCellComment() == null) {
            SXSSFDrawing p = cell.getSheet().createDrawingPatriarch();
            Comment commentValue = commentMap.computeIfAbsent(excelColumn, column -> {
                Comment c = p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, cell.getColumnIndex(), cell.getRowIndex(), cell.getColumnIndex() + 2, cell.getRowIndex() + 2));
                c.setString(new XSSFRichTextString(commentProperty.getComment()));
                c.setAuthor(commentProperty.getAuthor());
                return c;
            });
            cell.setCellComment(commentValue);
        }
    }

    private org.hzero.export.annotation.Comment getCommentInstance(Class<? extends org.hzero.export.annotation.Comment> commentClass) {
        try {
            return commentClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new CommonException("org.hzero.export.annotation.Comment class newInstance() failed", e);
        }
    }

    /**
     * 设置单元格的宽度
     *
     * @param sheet       SXSSFSheet
     * @param type        数据类型
     * @param columnIndex 列索引
     */
    protected void setCellWidth(SXSSFSheet sheet, String type, int columnIndex, int customWidth) {
        if (StringUtils.isBlank(type)) {
            return;
        }
        int columnWidth = 0;
        switch (type.toUpperCase()) {
            case "STRING":
                columnWidth = 22 * 256;
                break;
            case "FLOAT":
            case "DOUBLE":
            case "BIGDECIMAL":
            case "INT":
            case "INTEGER":
            case "LONG":
            case "BOOLEAN":
                columnWidth = 14 * 256;
                break;
            case "DATE":
                columnWidth = 21 * 256;
                break;
            default:
                columnWidth = 20 * 256;
                break;
        }
        sheet.setColumnWidth(columnIndex, columnWidth);
        if (customWidth != 0) {
            sheet.setColumnWidth(columnIndex, (int)(255.86 * customWidth + 184.27));
        }
    }


    /**
     * 设置标题单元格样式
     */
    protected void setTitleCellStyle() {
        Font font = workbook.createFont();
        font.setColor(Font.COLOR_NORMAL);
        font.setBold(true);

        titleCellStyle = workbook.createCellStyle();
        titleCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        titleCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleCellStyle.setFont(font);
        titleCellStyle.setAlignment(HorizontalAlignment.CENTER);
        titleCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    protected void setWorkbook(SXSSFWorkbook workbook) {
        this.workbook = workbook;
        setTitleCellStyle();
    }

//    protected CellStyle pickOutCellStyle(String pattern) {
//        Assert.isTrue(StringUtils.isNotBlank(pattern), "pattern should not be null");
//        return this.stylePool.computeIfAbsent(pattern, key -> {
//            CellStyle s = this.workbook.createCellStyle();
//            s.setDataFormat(this.workbook.createDataFormat().getFormat(pattern));
//            return s;
//        });
//    }

    protected Object doRender(Object value, Object rowData, List<Class<? extends ValueRenderer>> rendererTypes) {
        if (CollectionUtils.isNotEmpty(rendererTypes)) {
            for (Class<? extends ValueRenderer> rendererType : rendererTypes) {
                ValueRenderer renderer = this.renderers.computeIfAbsent(rendererType, key ->
                        Optional.ofNullable(key).map(type -> {
                            try {
                                return type.getConstructor().newInstance();
                            } catch (Exception e) {
                                logger.error("can not create renderer!", e);
                                return null;
                            }
                        }).orElse(null)
                );

                if (renderer != null) {
                    value = renderer.render(value, rowData);
                }
            }
        }
        return value;
    }


}
