package com.gridnine.platform.elsa.core.storage;

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject;

public interface AggregatedDataProvider<A extends BaseIntrospectableObject, I extends BaseIdentity> {
    Class<A> getObjectClass();
    String getAggregatedData(A object, OperationContext<I> context) throws Exception;
}
