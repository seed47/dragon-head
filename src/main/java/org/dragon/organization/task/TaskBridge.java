package org.dragon.organization.task;

import org.dragon.character.task.Task;

/**
 * TaskBridge 任务桥接器
 * 连接 OrganizationTask 和 CharacterTask
 *
 * @author wyj
 * @version 1.0
 */
public interface TaskBridge {

    /**
     * 将 SubTask 提交给 Character 执行
     *
     * @param subTask 子任务
     * @param characterId Character ID
     * @return Character Task ID
     */
    String submitToCharacter(SubTask subTask, String characterId);

    /**
     * 获取 Character Task
     *
     * @param characterTaskId Character Task ID
     * @return Character Task
     */
    Task getCharacterTask(String characterTaskId);

    /**
     * 获取 Character Task 的状态
     *
     * @param characterTaskId Character Task ID
     * @return 组织任务状态
     */
    OrgTaskStatus getCharacterTaskStatus(String characterTaskId);

    /**
     * 取消 Character Task
     *
     * @param characterTaskId Character Task ID
     * @return 是否成功
     */
    boolean cancelCharacterTask(String characterTaskId);
}
