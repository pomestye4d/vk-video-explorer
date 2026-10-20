/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

import com.gridnine.platform.elsa.common.core.utils.TypedParameterId;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingDescription;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingGroupDescription;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingSubscriptionDescription;
import com.gridnine.platform.elsa.common.meta.remoting.ServiceDescription;

public class StandardRemotingParameters {

    public static final TypedParameterId<RemotingDescription> REMOTING_DESCRIPTION = () -> "REMOTING_DESCRIPTION";
    public static final TypedParameterId<ServiceDescription> SERVICE_DESCRIPTION = () -> "SERVICE_DESCRIPTION";

    public static final TypedParameterId<RemotingSubscriptionDescription> SUBSCRIPTION_DESCRIPTION = () -> "SUBSCRIPTION_DESCRIPTION";

    public static final TypedParameterId<RemotingGroupDescription> GROUP_DESCRIPTION = () -> "GROUP_DESCRIPTION";


}
