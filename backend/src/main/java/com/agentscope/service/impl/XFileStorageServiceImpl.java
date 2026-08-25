package com.agentscope.service.impl;

import cn.hutool.core.util.StrUtil;
import com.agentscope.model.entity.XFileRecord;
import com.agentscope.repository.XFileRecordRepository;
import com.agentscope.service.XFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 统一文件存储服务实现（x-file-storage 本地模式）。
 * <p>
 * 存储布局：{storageRoot}/{module}/{bizId}/{文件名}；业务表保存相对存储key。
 * 每次上传经 MongoFileRecorder 自动登记至 xfile_records 集合；
 * 删除优先走 x-file-storage，再做磁盘兜底，最后清理登记记录；
 * 历史绝对路径（存量数据）自动识别并原样透传，不做迁移。
 */
@Slf4j
@Service
public class XFileStorageServiceImpl implements XFileStorageService {

    /** x-file-storage 核心服务（由 @EnableFileStorage 自动装配） */
    private final org.dromara.x.file.storage.core.FileStorageService xfs;

    /** 文件登记记录仓库 */
    private final XFileRecordRepository recordRepository;

    /** 存储根目录，与静态资源映射 /files/** 指向同一目录 */
    @Value("${dromara.x-file-storage.local-plus[0].storage-path:/data/agent-scope/storage/}")
    private String storageRoot;

    /** 旧版文档根目录（仅用于存量兼容清理） */
    @Value("${app.file.legacy-base-path:/data/agent-scope/files}")
    private String legacyBasePath;

    public XFileStorageServiceImpl(org.dromara.x.file.storage.core.FileStorageService xfs,
                                   XFileRecordRepository recordRepository) {
        this.xfs = xfs;
        this.recordRepository = recordRepository;
    }

    @Override
    public String store(MultipartFile file, String module, String bizId) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String saveName = UUID.randomUUID() + (StrUtil.isNotBlank(extension) ? "." + extension : "");

        FileInfo info = xfs.of(file)
                .setPath(module + "/" + (bizId != null ? bizId + "/" : ""))
                .setSaveFilename(saveName)
                .putAttr("module", module)
                .putAttr("bizId", bizId)
                .upload();
        log.info("文件已存储: module={}, bizId={}, key={}", module, bizId, toKey(info));
        return toKey(info);
    }

    @Override
    public String storeBytes(byte[] bytes, String module, String bizId, String saveFilename) {
        MultipartFile wrapper = new InMemoryMultipartFile(bytes, saveFilename);
        FileInfo info = xfs.of(wrapper)
                .setPath(module + "/" + (bizId != null ? bizId + "/" : ""))
                .setSaveFilename(saveFilename)
                .putAttr("module", module)
                .putAttr("bizId", bizId)
                .upload();
        log.info("字节数据已存储: module={}, bizId={}, size={}", module, bizId, bytes.length);
        return toKey(info);
    }

    @Override
    public void delete(String keyOrPath) {
        if (StrUtil.isBlank(keyOrPath)) {
            return;
        }
        try {
            if (isLegacyAbsolute(keyOrPath)) {
                if (Files.deleteIfExists(Paths.get(keyOrPath))) {
                    log.info("存量文件已删除: {}", keyOrPath);
                }
                return;
            }
            List<XFileRecord> records = recordRepository.findAllByStorageKey(keyOrPath);
            for (XFileRecord r : records) {
                try {
                    if (StrUtil.isNotBlank(r.getUrl())) {
                        xfs.delete(r.getUrl());
                    }
                } catch (Exception e) {
                    log.warn("x-file-storage删除失败: url={}", r.getUrl(), e);
                }
            }
            // 物理兜底：确保磁盘不留孤儿文件
            Files.deleteIfExists(resolve(keyOrPath));
            if (!records.isEmpty()) {
                recordRepository.deleteAll(records);
            }
            log.info("存储文件已删除: {}", keyOrPath);
        } catch (Exception e) {
            log.error("删除存储文件失败: {}", keyOrPath, e);
        }
    }

    @Override
    public Path resolve(String keyOrPath) {
        if (isLegacyAbsolute(keyOrPath)) {
            return Paths.get(keyOrPath);
        }
        return Paths.get(storageRoot, keyOrPath);
    }

    @Override
    public boolean isLegacyAbsolute(String path) {
        return StrUtil.isBlank(path) || path.startsWith("/") || path.contains(":\\");
    }

    @Override
    public String storeFile(MultipartFile file, String knowledgeBaseId) throws IOException {
        return store(file, "doc", knowledgeBaseId);
    }

    @Override
    public Path getFilePath(String keyOrPath) {
        return resolve(keyOrPath);
    }

    @Override
    public void deleteFile(String keyOrPath) {
        delete(keyOrPath);
    }

    @Override
    public void deleteKnowledgeBaseFiles(String knowledgeBaseId) {
        // 新布局：storage/doc/{kbId}/ 整目录删除 + 登记记录逐条清理
        deleteDirectory(Paths.get(storageRoot, "doc", knowledgeBaseId));
        recordRepository.findAllByStorageKeyStartingWith("doc/" + knowledgeBaseId + "/")
                .forEach(r -> delete(r.getStorageKey()));
        // 旧布局兜底：{legacyBasePath}/{kbId}/
        deleteDirectory(Paths.get(legacyBasePath, knowledgeBaseId));
    }

    /** 由FileInfo还原存储key（去掉basePath前缀后拼接文件名） */
    private String toKey(FileInfo info) {
        String base = info.getBasePath() == null ? "" : info.getBasePath();
        String path = StrUtil.removePrefix(info.getPath() == null ? "" : info.getPath(), base);
        return path + info.getFilename();
    }

    /** 提取小写扩展名（无扩展名返回空串） */
    private String getExtension(String filename) {
        if (StrUtil.isBlank(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /** 递归删除目录（含全部子项） */
    private void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            log.warn("删除目录失败: {}", dir, e);
        }
    }

    /**
     * 内存MultipartFile适配器：将byte[]包装为Spring标准上传接口，
     * 使x-file-storage能像处理普通上传一样处理程序生成的字节数据。
     */
    private static class InMemoryMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String filename;

        InMemoryMultipartFile(byte[] bytes, String filename) {
            this.bytes = bytes != null ? bytes : new byte[0];
            this.filename = filename != null ? filename : "file.bin";
        }

        @Override public String getName() { return "file"; }

        @Override public String getOriginalFilename() { return filename; }

        @Override public String getContentType() { return "application/octet-stream"; }

        @Override public boolean isEmpty() { return bytes.length == 0; }

        @Override public long getSize() { return bytes.length; }

        @Override public byte[] getBytes() { return bytes; }

        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

        @Override public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
