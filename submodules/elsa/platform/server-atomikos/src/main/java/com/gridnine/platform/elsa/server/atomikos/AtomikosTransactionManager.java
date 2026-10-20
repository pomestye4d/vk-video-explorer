/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.server.atomikos;

import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAndArgument;
import com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAndArgument;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionContext;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AtomikosTransactionManager implements ElsaTransactionManager {

    private final static ThreadLocal<ElsaTransactionContext> contexts = new ThreadLocal<>();

    private final TransactionTemplate template;
    private final TransactionTemplate readOnlyTemplate;

    public AtomikosTransactionManager(PlatformTransactionManager pm) {
        template = new TransactionTemplate(pm);
        readOnlyTemplate = new TransactionTemplate(pm);
        readOnlyTemplate.setReadOnly(true);
    }


    @Override
    public ElsaTransactionContext getCurrentContext() {
        return contexts.get();
    }

    @Override
    public void withTransaction(RunnableWithExceptionAndArgument<ElsaTransactionContext> func) {
        withTransaction((context) -> {
            func.run(context);
            return (Void) null;
        }, false);
    }

    @Override
    public <P> P withTransaction(CallableWithExceptionAndArgument<P, ElsaTransactionContext> func, boolean readonly) {
        return ExceptionUtils.wrapException(() -> {
            var owner = contexts.get() == null;
            try {
                if (owner) {
                    var context = new ElsaTransactionContext();
                    contexts.set(context);
                    return (readonly ? readOnlyTemplate : template).execute((status) ->
                            ExceptionUtils.wrapException(() -> func.call(context)));

                } else {
                    return func.call(contexts.get());
                }
            } finally {
                if (owner) {
                    var callbacks = contexts.get().getPostCommitCallbacks();
                    contexts.remove();
                    callbacks.forEach(ExceptionUtils::wrapException);
                }
            }
        });
    }
}
