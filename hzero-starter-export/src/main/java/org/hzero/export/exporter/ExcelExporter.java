package org.hzero.export.exporter;

import io.choerodon.core.exception.CommonException;
import org.apache.commons.codec.Charsets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hzero.core.message.MessageAccessor;
import org.hzero.core.util.FilenameUtils;
import org.hzero.core.util.Pair;
import org.hzero.export.ExcelFillerHolder;
import org.hzero.export.IExcelExporter;
import org.hzero.export.filler.ExcelFiller;
import org.hzero.export.util.ShardUtils;
import org.hzero.export.vo.ExportColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Deprecated
public class ExcelExporter implements IExcelExporter {

    public static final int MAX_ROW = 1000000;

    private static final String CONTENT_TYPE_TXT_FILE = "text/plain";
    private static final String CONTENT_TYPE_ZIP_FILE = "application/zip";
    private static final String CONTENT_TYPE_EXCEL_FILE = "application/vnd.ms-excel";

    private static final int READ_BUFFER_SIZE = 4 * 1024 * 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelExporter.class);

    // 工作铺
    private List<SXSSFWorkbook> workbooks;

    // 导出列信息
    private ExportColumn root;

    // 数据填充器
    private List<ExcelFiller> excelFillers;

    private String fillerType;

    private int singleExcelMaxRow;

    private int singleSheetMaxRow;

    private int singleExcelMaxSheetNum;

    private File temp;

    private InputStream inputStream;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private String fileName;

    private int pageExportDataIndex;

    private String errorMessage;

    /**
     * @param root                   导出列
     * @param fillerType             数据填充方式
     * @param singleExcelMaxSheetNum 单excel最大sheet数
     * @param singleSheetMaxRow      单sheet页最大行数
     * @param response               HttpServletResponse
     */
    public ExcelExporter(ExportColumn root, String fillerType,
                         int singleExcelMaxSheetNum, int singleSheetMaxRow,
                         HttpServletResponse response) {
        this.workbooks = new ArrayList<>();
        this.excelFillers = new ArrayList<>();
        this.fillerType = fillerType;
        this.singleExcelMaxSheetNum = singleExcelMaxSheetNum;
        this.singleSheetMaxRow = singleSheetMaxRow;
        this.singleExcelMaxRow = singleSheetMaxRow * this.singleExcelMaxSheetNum;
        this.root = root;
        this.request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        this.response = response;
        addShard();
    }

    /**
     * @param root       导出列
     * @param fillerType 数据填充方式
     * @param response   HttpServletResponse
     */
    public ExcelExporter(ExportColumn root, String fillerType, HttpServletResponse response) {
        this(root, fillerType, 5, ExcelExporter.MAX_ROW, response);
    }

    /**
     * @param root     导出列
     * @param filler   数据填充器
     * @param response HttpServletResponse
     */
    public ExcelExporter(ExportColumn root, ExcelFiller filler, HttpServletResponse response) {
        this.workbooks = new ArrayList<>();
        this.excelFillers = new ArrayList<>();
        this.fillerType = filler.getFillerType();
        SXSSFWorkbook shard = new SXSSFWorkbook();
        this.workbooks.add(shard);
        this.excelFillers.add(filler);
        this.singleExcelMaxSheetNum = 5;
        this.singleSheetMaxRow = ExcelExporter.MAX_ROW;
        this.singleExcelMaxRow = ExcelExporter.MAX_ROW * this.singleExcelMaxSheetNum;
        this.root = root;
        this.request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        this.response = response;
    }

    private void addShard() {
        SXSSFWorkbook shard = new SXSSFWorkbook();
        this.workbooks.add(shard);
        ExcelFiller excelFiller = ExcelFillerHolder.getExcelFiller(fillerType, root, shard);
        Assert.notNull(excelFiller, "export.filler.not-found");
        excelFiller.configure(singleSheetMaxRow);
        this.excelFillers.add(excelFiller);
    }

    /**
     * 只能扩容，无法缩容
     *
     * @param size
     */
    private void splitShard(int size) {
        int addSize = size - workbooks.size();
        for (int i = 0; i < addSize; i++) {
            addShard();
        }
    }

    /**
     * 填充表数据
     *
     * @param exportData 要填充的数据
     */
    @Override
    public void fillSheet(List<?> exportData) {

        if (CollectionUtils.isEmpty(exportData) || exportData.get(0) == null) {
            return;
        }

        try {
            fillSheetWithShard(exportData);
        } catch (Exception e) {
            LOGGER.error("fill data occurred error.", e);
        }
    }

    /**
     * 填充表数据
     *
     * @param exportData 要填充的数据
     */
    public void fillSheetForPage(List<?> exportData) {

        if (CollectionUtils.isEmpty(exportData) || exportData.get(0) == null) {
            return;
        }

        if (exportData.size() > singleSheetMaxRow) {
            throw new CommonException("The number of single pagination queries should not be greater than the maximum number of rows in a single sheet page(@{pageSize="+exportData.size()+"} > @{singleSheetMaxRow=" + singleSheetMaxRow + "})." +
                    "Please try to configure a larger maximum number of rows in a single sheet page or reduce the size of a single page.");
        }

        int shardNum = pageExportDataIndex++ / singleExcelMaxSheetNum + 1;
        splitShard(shardNum);
        doFillData(workbooks.get(shardNum - 1), root, excelFillers.get(shardNum - 1), exportData, true);
    }

    private void fillSheetWithShard(List<?> exportData) {
        int sum = exportData.size();
        Pair<Integer, Integer> pair = ShardUtils.shard(sum, singleExcelMaxRow);
        int shardNum = pair.getFirst();
        int lastNum = pair.getSecond();
        splitShard(shardNum);
        List<List<?>> shardData = ShardUtils.prepareShardData(exportData, shardNum, singleExcelMaxRow, lastNum);
        fillDataWithShard(workbooks, root, excelFillers, shardData);
    }

    private void fillDataWithShard(List<SXSSFWorkbook> workbooks, ExportColumn root, List<ExcelFiller> excelFillers, List<List<?>> shardData) {
        if (workbooks.size() > excelFillers.size()) {
            throw new IllegalStateException("分片异常");
        }
        for (int i = 0; i < workbooks.size(); i++) {
            ExcelFiller excelFiller = excelFillers.get(i);
            SXSSFWorkbook workbook = workbooks.get(i);
            List<?> exportData = shardData.get(i);
            doFillData(workbook, root, excelFiller, exportData, false);
        }
    }

    private void doFillData(SXSSFWorkbook workbook, ExportColumn root, ExcelFiller excelFiller, List<?> exportData, boolean paging) {
        excelFiller.fillDataWithShard(workbook, root, exportData, paging);
    }

    /**
     * 导出模板，仅设置标题
     */
    @Override
    public void fillTitle() {
        SXSSFWorkbook workbook = workbooks.get(0);
        if (workbook.getNumberOfSheets() == 0) {
            excelFillers.get(0).createSheetAndTitle(workbook, root);
        }
    }

    /**
     * 设置http请求报文为下载文件
     */
    public void setExcelHeader() {
        String contentType = getContentType();
        String filename = getDownloadFileName();
        String encodeFileName = null;
        try {
            encodeFileName = FilenameUtils.encodeFileName(request, filename);
        } catch (IOException e) {
            LOGGER.error("encode file name failed.", e);
        }
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment;filename=\"%s\"", encodeFileName));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setContentType(contentType + "; charset=" + Charsets.UTF_8.displayName());
        response.setCharacterEncoding(Charsets.UTF_8.displayName());
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName == null ? root.getTitle() : fileName;
    }

    private void writeBytes(OutputStream outputStream) throws IOException {
        try {
            if (!StringUtils.isEmpty(errorMessage)) {
                outputStream.write(errorMessage.getBytes());
                return;
            }
            if (workbooks.size() == 1) {
                workbooks.get(0).write(outputStream);
            } else {
                writeZipBytes(outputStream);
            }
        } catch (IOException e) {
            LOGGER.error("IO exception when write response", e);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }

    }

    private void writeOutputStream(InputStream inputStream, ServletOutputStream outputStream) throws IOException {
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer, 0, READ_BUFFER_SIZE)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
    }

    private void writeZipBytes(OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(outputStream);
            ZipEntry zipEntry = null;
            for (int i = 0; i < workbooks.size(); i++) {
                SXSSFWorkbook workbook = workbooks.get(i);
                zipEntry = new ZipEntry(root.getTitle() + "-" + (i + 1) + EXCEL_SUFFIX);
                zipOutputStream.putNextEntry(zipEntry);
                workbook.write(zipOutputStream);
            }
            zipOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipOutputStream != null) {
                zipOutputStream.closeEntry();
                zipOutputStream.close();
            }
        }

    }

    private File buildTempFile() throws IOException {
        File tmp = null;
        FileOutputStream fos = null;
        try {
            if (workbooks.size() == 1) {
                tmp = new File(UUID.randomUUID() + EXCEL_SUFFIX);
                boolean create = tmp.createNewFile();
                Assert.isTrue(create, "create tmp file error!");
                fos = new FileOutputStream(tmp);
                workbooks.get(0).write(fos);
                fos.flush();
            } else {
                tmp = new File(UUID.randomUUID() + ZIP_SUFFIX);
                boolean create = tmp.createNewFile();
                Assert.isTrue(create, "create tmp file error!");
                fos = new FileOutputStream(tmp);
                writeZipBytes(fos);
                fos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return tmp;
    }

    private byte[] readDataFromTempFile(File tmp) throws IOException {
        FileInputStream fis = null;
        byte[] buffer = null;
        try {
            fis = new FileInputStream(tmp);
            int max = fis.available();
            buffer = new byte[max];
            fis.read(buffer, 0, max);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return buffer;
    }

    private InputStream readInputStreamFromTempFile(File temp) throws IOException {
        if (inputStream == null) {
            inputStream = new FileInputStream(temp);
        }
        return inputStream;
    }

    /**
     * 下载Excel
     */
    public void downloadExcel() throws IOException {
        setExcelHeader();
        writeBytes(response.getOutputStream());
    }

    /**
     * 获取文件数据
     *
     * @return
     * @throws IOException
     */
    public byte[] readFileBytes() throws IOException {
        if (temp == null) {
            temp = buildTempFile();
        }
        return readDataFromTempFile(temp);
    }

    /**
     * 获取文件输入流, 便于上传文件优化
     *
     * @return
     * @throws IOException
     */
    public InputStream readInputStream() throws IOException {
        if (temp == null) {
            temp = buildTempFile();
        }
        return readInputStreamFromTempFile(temp);
    }

    /**
     * 文件输入流处理结束后的清理逻辑
     *
     * @throws IOException
     */
    public void cleanTempFile() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        if (temp != null) {
            boolean del = temp.delete();
            Assert.isTrue(del, "delete tmp file error!");
        }
    }

    public String getDownloadFileName(){
        String suffix = getFileSuffix();
        return TXT_SUFFIX.equals(suffix) ? "error" + suffix : getFileName() + suffix;
    }

    public String getFileSuffix(){
        if (!StringUtils.isEmpty(errorMessage)) {
            return TXT_SUFFIX;
        }
        return isZip() ? ZIP_SUFFIX : EXCEL_SUFFIX;
    }

    public String getContentType(){
        if (!StringUtils.isEmpty(errorMessage)) {
            return CONTENT_TYPE_TXT_FILE;
        }
        return isZip() ? CONTENT_TYPE_ZIP_FILE : CONTENT_TYPE_EXCEL_FILE;
    }

    public boolean isZip() {
        return workbooks.size() > 1;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = MessageAccessor.getMessage(errorMessage).desc();
    }

    @Override
    public void setError(String errorMessage) {
        setErrorMessage(errorMessage);
    }

    @Override
    public String getError() {
        return errorMessage;
    }

    @Override
    public InputStream export() throws IOException {
        return readInputStream();
    }

    @Override
    public byte[] exportBytes() throws IOException {
        return readFileBytes();
    }

    @Override
    public void export(OutputStream outputStream) throws IOException {
        writeBytes(outputStream);
    }

    @Override
    public String getTitle() {
        return root.getTitle();
    }

    @Override
    public String getOutputFileSuffix() {
        return getFileSuffix();
    }

    @Override
    public void close() throws Exception {
        cleanTempFile();
    }
}
