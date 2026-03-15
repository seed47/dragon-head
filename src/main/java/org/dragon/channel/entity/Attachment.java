package org.dragon.channel.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:07
 * Update Date Time:
 *
 */
@Data
@NoArgsConstructor
public class Attachment {
    private String fileUrl;   // 文件的本地路径或可下载的网络 URL
    private String mimeType;  // 文件类型，如 "image/png", "audio/ogg"
    private String fileName;  // 文件名
}
