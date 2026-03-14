package org.dragon.gateway;

import org.dragon.channel.entity.NormalizedMessage;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:10
 * Update Date Time:
 *
 */
public interface Gateway {
    void dispatch(NormalizedMessage message);
}
