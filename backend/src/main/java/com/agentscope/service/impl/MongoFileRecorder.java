package com.agentscope.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentscope.model.entity.XFileRecord;
import com.agentscope.repository.XFileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * x-file-storage 文件记录器（MongoDB实现）
 * <p>
 * 上传时登记文件记录（URL、存储key、业务归属），删除记录时可据此清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoFileRecorder implements FileRecorder {

    private final XFileRecordRepository repository;

    @Override
    public boolean save(FileInfo info) {
        if (info == null) return false;
        try {
            XFileRecord record = XFileRecord.builder()
                    .url(info.getUrl())
                    .filename(info.getFilename())
                    .originalFilename(info.getOriginalFilename())
                    .size(info.getSize())
                    .platform(info.getPlatform())
                    .storageKey(relativeKey(info))
                    .module(metadata(info, "module"))
                    .bizId(metadata(info, "bizId"))
                    .fileInfoJson(toJson(info))
                    .build();
            repository.save(record);
            return true;
        } catch (Exception e) {
            log.error("保存文件记录失败: url={}", info.getUrl(), e);
            return false;
        }
    }

    @Override
    public void update(FileInfo info) {
        if (info == null || StrUtil.isBlank(info.getUrl())) return;
        Optional<XFileRecord> found = repository.findFirstByUrlOrderByIdDesc(info.getUrl());
        found.ifPresent(r -> {
            r.setSize(info.getSize());
            r.setFileInfoJson(toJson(info));
            repository.save(r);
        });
    }

    @Override
    public FileInfo getByUrl(String url) {
        return repository.findFirstByUrlOrderByIdDesc(url)
                .map(r -> {
                    try {
                        return JSONUtil.toBean(r.getFileInfoJson(), FileInfo.class);
                    } catch (Exception e) {
                        // 记录JSON缺失时构造最小可用信息
                        FileInfo info = new FileInfo();
                        info.setUrl(r.getUrl());
                        info.setFilename(r.getFilename());
                        info.setSize(r.getSize());
                        info.setPlatform(r.getPlatform());
                        return info;
                    }
                })
                .orElse(null);
    }

    @Override
    public boolean delete(String url) {
        repository.findFirstByUrlOrderByIdDesc(url).ifPresent(repository::delete);
        return true;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {
        // 未启用分片上传，暂不记录
    }

    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        // 未启用分片上传，暂不记录
    }

    /** 由FileInfo还原存储key（相对storage-root） */
    private String relativeKey(FileInfo info) {
        String base = info.getBasePath() == null ? "" : info.getBasePath();
        String path = StrUtil.removePrefix(info.getPath() == null ? "" : info.getPath(), base);
        return path + info.getFilename();
    }

    /**
     * 读取业务扩展属性。
     * local-1本地平台不支持metadata，因此上传时通过attr传递业务信息（module/bizId），
     * attr不参与存储层行为，仅随FileInfo透传给记录器。
     */
    private String metadata(FileInfo info, String key) {
        Map<String, Object> attr = info.getAttr();
        if (attr == null || attr.isEmpty()) return null;
        Object v = attr.get(key);
        return v != null ? v.toString() : null;
    }



    private String toJson(FileInfo info) {
        try {
            return JSONUtil.toJsonStr(info);
        } catch (Exception e) {
            return null;
        }
    }
}
