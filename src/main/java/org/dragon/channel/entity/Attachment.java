package org.dragon.channel.entity;

import lombok.Getter;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:07
 * Update Date Time:
 *
 */
@Getter
public class Attachment {
    private final String fileUrl;   // 文件的本地路径或可下载的网络 URL
    private final String mimeType;  // 文件类型，如 "image/png", "audio/ogg"
    private final String fileName;  // 文件名

    public Attachment(String fileUrl, String mimeType, String fileName) {
        this.fileUrl = fileUrl;
        this.mimeType = mimeType;
        this.fileName = fileName;
    }

}
