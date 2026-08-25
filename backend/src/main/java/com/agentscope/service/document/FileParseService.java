package com.agentscope.service.document;

import java.io.File;
import java.util.List;

/**
 * 文档解析服务。
 * <p>
 * 支持PDF（按页）、Word、TXT等格式的全文与分页文本提取。
 */
public interface FileParseService {

    /** 解析文件全文文本 */
    String parseFile(File file, String fileType);

    /** 按页解析文件文本（仅PDF支持分页，其他类型返回单元素列表） */
    List<String> parseFileByPage(File file, String fileType);

    /** 提取小写扩展名 */
    String getFileExtension(String fileName);
}
