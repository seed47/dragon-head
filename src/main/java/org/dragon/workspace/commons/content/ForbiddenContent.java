package org.dragon.workspace.commons.content;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 禁止项内容
 * 用于存储禁止的行为或内容
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForbiddenContent {

    /**
     * 禁止项列表
     */
    private List<String> items;

    /**
     * 禁止原因
     */
    private String reason;
}