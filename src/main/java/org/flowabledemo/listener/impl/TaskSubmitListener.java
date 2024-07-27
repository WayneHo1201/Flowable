package org.flowabledemo.listener.impl;

import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowabledemo.listener.AbstractTaskCompleteListener;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Slf4j
public class TaskSubmitListener extends AbstractTaskCompleteListener {

    @Override
    protected Consumer<DelegateTask> createTaskConsumer() {
        return delegateTask -> {
            log.info("handle submit process," +
                    " id = {}, candidates = {}", delegateTask.getId(), delegateTask.getCandidates());
        };
    }
}
