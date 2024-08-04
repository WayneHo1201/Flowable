package com.fwd.fsm.flowable.listener;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

import java.util.function.Consumer;


@Slf4j
public abstract class AbstractTaskCompleteListener implements TaskListener {
    protected abstract Consumer<DelegateTask> createTaskConsumer();

    @Override
    @SneakyThrows
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            throw new IllegalAccessException("This class does not allow to access except for complete event");
        }
        try {
            Consumer<DelegateTask> taskConsumer = createTaskConsumer();
            if (taskConsumer == null) {
                log.warn("No processing logic created, using default processing");
                // todo default handling
                return;
            }
            taskConsumer.accept(delegateTask);
        } catch (Exception e) {
            log.error("workflow was blocked, id = {}, assignee = {}", delegateTask.getId(), delegateTask.getAssignee(), e);
        }
    }

}
