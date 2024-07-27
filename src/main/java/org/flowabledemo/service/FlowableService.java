package org.flowabledemo.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.*;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlowableService {
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private IdentityService identityService;


    public Map<String, String> deployFlow(String flowName) {
        return null;
    }

    public List<Task> getTaskByAssignee(String name) {
        return taskService.createTaskQuery()
                .taskAssignee(name)
                .list();
    }

    public ProcessDefinition getProcdef(String key, int version) {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).list();
        return list.stream().filter(processDefinition -> version == processDefinition.getVersion()).findFirst().orElse(null);
    }

    public String startFlow(String id) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(id);
        log.info("start flow successfully! InstanceId = {}", processInstance.getId());
        return processInstance.getId();
    }

    public String claimTask(String taskId, String userId) {
        if (!validateTask(taskId, userId)) {
            throw new FlowableIllegalArgumentException("cannot assign task " + taskId + "to " + userId);
        }
        taskService.claim(taskId, userId);
        return taskId;
    }

    private boolean validateTask(String taskId, String userId) {
        List<Task> taskByAssignee = getTaskByAssignee(userId);
        return taskByAssignee.stream().filter(task -> task.getId().equals(taskId)).findFirst().orElse(null) != null;
    }

    public void completeFlow(String taskId) {
        taskService.complete(taskId);
    }


    public List<String> getTaskListByName(String name) {
        return taskService.createTaskQuery()
                .taskCandidateOrAssigned(name)
                .list()
                .stream()
                .map(TaskInfo::getId)
                .collect(Collectors.toList());
    }
}
