package org.dragon.channel.file;

import org.dragon.channel.entity.NormalizedFile;
import org.dragon.channel.enums.FileSource;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/18 0:11
 * Update Date Time:
 *
 * @see
 */
public interface FileStorageAdapter {
    /**
     * 当前适配器处理哪种类型的数据源
     */
    FileSource getSupportedSource();
    /**
     * 执行具体的抓取/下载动作
     */
    NormalizedFile download(NormalizedFile fileMeta) throws Exception;
    /**
     * 执行具体的保存/上传动作
     */
    NormalizedFile upload(NormalizedFile file) throws Exception;
}
