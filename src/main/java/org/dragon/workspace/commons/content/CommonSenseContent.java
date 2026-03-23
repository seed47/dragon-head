package org.dragon.workspace.commons.content;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CommonSense 内容主类
 * 存储结构化的规则内容
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonSenseContent {

    /**
     * 内容类型
     */
    @SerializedName("type")
    private ContentType type;

    /**
     * 内容描述（可选）
     */
    @SerializedName("description")
    private String description;

    /**
     * 内容数据
     * 类型根据 type 不同而不同
     */
    @SerializedName("data")
    private Object data;
}