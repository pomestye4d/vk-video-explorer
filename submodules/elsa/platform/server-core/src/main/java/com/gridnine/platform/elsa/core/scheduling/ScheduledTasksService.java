/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.core.scheduling;

import com.gridnine.platform.elsa.common.core.lock.LockManager;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;

@Service
public class ScheduledTasksService {

    private final transient Logger log = LoggerFactory.getLogger(getClass());

    private final ThreadPoolTaskScheduler scheduler;

    private final HashMap<String, ScheduledFuture<?>> class2future = new HashMap<>();

    private final ListableBeanFactory factory;

    private final LockManager lockManager;

    @Autowired
    public ScheduledTasksService(ThreadPoolTaskScheduler scheduler, ListableBeanFactory factory, LockManager lockManager) {
        this.scheduler = scheduler;
        this.factory = factory;
        this.lockManager = lockManager;
    }
    public <P, T extends ScheduledTask<P>> void schedule(@NonNull final Class<T> clazz, @Nullable P parameters,
                             @NonNull final String cronExp) {
        schedule(clazz, parameters, cronExp, false);
    }
    public <P, T extends ScheduledTask<P>> void schedule(@NonNull final Class<T> clazz, @Nullable P parameters,
                             @NonNull final String cronExp, @NonNull final boolean skipIfTaskIsAlreadyScheduled) {
        lockManager.withLock(getLockKey(clazz), () -> {
            ScheduledTask<P> task = factory.getBeansOfType(clazz).values().stream().findFirst().orElse(null);
            if (task == null) {
                task = clazz.getDeclaredConstructor().newInstance();
            }
            ScheduledFuture<?> existing = class2future.get(clazz.getName());
            if (existing != null) {
                if (skipIfTaskIsAlreadyScheduled) {
                    log.warn(String.format("task %s already scheduled, skipping", clazz.getName()));
                    return;
                }
                unscheduleInternal(clazz);
            }
            TaskExecutionContext<P> context = new TaskExecutionContext<>();
            context.setParameters(parameters);
            RunnableTask<P> rt = new RunnableTask<>(task, context);
            class2future.put(clazz.getName(),
                    scheduler.schedule(rt, new CronTrigger(cronExp)));
            log.debug("scheduled task " + task.getClass().getName());
        });
    }

    public void unschedule(@NonNull final Class<? extends ScheduledTask<?>> clazz) {
        lockManager.withLock(getLockKey(clazz), () -> unscheduleInternal(clazz));
    }

    private void unscheduleInternal(final Class<? extends ScheduledTask<?>> clazz) throws Exception {
        unscheduleInternal(clazz.getName());
    }
    private void unscheduleInternal(final String className) throws Exception {
        ScheduledFuture<?> future = class2future.remove(className);
        if (future != null) {
            future.cancel(true);
            try {
                future.get();
            } catch (CancellationException | InterruptedException e) {
                //noops
            }
            log.debug("unscheduled task " + className);
            return;
        }
        log.debug(String.format("task %s not scheduled, skipping", className));
    }

    private String getLockKey(Class<?> cls) {
        return String.format("scheduled-task-%s", cls.getName());
    }

    @PreDestroy
    public void shutdown() throws Exception{
        for(String it: new HashSet<>(class2future.keySet())) {
            unscheduleInternal(it);
        }
    }

    static class RunnableTask<P> implements Runnable {
        private final ScheduledTask<P> delegate;

        private final TaskExecutionContext<P> context;

        private final transient Logger log = LoggerFactory.getLogger(getClass());

        public RunnableTask(ScheduledTask<P> delegate, TaskExecutionContext<P> context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public void run() {
            try {
                delegate.doJob(this.context);
            } catch (Throwable e) {
                log.error("unable to execute task", e);
            }
        }
    }
}
