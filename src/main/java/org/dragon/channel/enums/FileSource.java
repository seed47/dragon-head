package org.dragon.channel.enums;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/18 0:07
 * Update Date Time:
 */
public enum FileSource {
    FEISHU_IM("飞书聊天"),
    FEISHU_DRIVE("飞书云空间"),
    S3("AWS S3"),
    LOCAL("本地文件系统"),
    WEB_URL("公开网络链接");

    private final String description;

    FileSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
