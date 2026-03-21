package org.dragon.channel;

import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.entity.NormalizedFile;
import org.dragon.channel.enums.FileSource;
import org.dragon.channel.file.FileStorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/18 0:33
 * Update Date Time:
 */
@Service
@Slf4j
public class FileManager {

    private final Map<FileSource, FileStorageAdapter> adapterMap = new EnumMap<>(FileSource.class);

    @Autowired
    public FileManager(List<FileStorageAdapter> adapters) {
        for (FileStorageAdapter adapter : adapters) {
            adapterMap.put(adapter.getSupportedSource(), adapter);
        }
    }

    public NormalizedFile download(NormalizedFile fileMeta) throws Exception {
        FileStorageAdapter adapter = getAdapter(fileMeta.getFileSource());
        return adapter.download(fileMeta);
    }

    public NormalizedFile saveFile(NormalizedFile file) throws Exception {
        FileStorageAdapter adapter = getAdapter(file.getFileSource());
        return adapter.upload(file);
    }

    private FileStorageAdapter getAdapter(FileSource source) {
        FileStorageAdapter adapter = adapterMap.get(source);
        if (adapter == null) {
            throw new IllegalArgumentException("系统尚未配置处理该数据源的适配器: " + source);
        }
        return adapter;
    }

}
