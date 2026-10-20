/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.scheduling;

public interface ScheduledTask<P> {
    void doJob(TaskExecutionContext<P> context) throws Exception;
}
