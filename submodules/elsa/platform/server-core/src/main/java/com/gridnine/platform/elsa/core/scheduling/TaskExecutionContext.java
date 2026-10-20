/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.scheduling;

public class TaskExecutionContext<P> {
    private P parameters;

    public void setParameters(P parameters) {
        this.parameters = parameters;
    }

    public P getParameters() {
        return parameters;
    }
}
