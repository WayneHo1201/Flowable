package org.flowabledemo;


import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class FlowableDemoTest {
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;

    /**
     * deploy a workflow
     * <p>
     * select * from ACT_GE_BYTEARRAY;
     * select * from ACT_RE_DEPLOYMENT;
     * select * from ACT_RE_PROCDEF;
     */
    @Test
    void deployFlow() {
        Deployment firstDeployment = repositoryService
                .createDeployment()
                .addClasspathResource("process/FlowableDemo.bpmn20.xml")
                .name("Movement demo")
                .deploy();
        log.info("deploy {} successfully! id = {}", firstDeployment.getName(), firstDeployment.getId());
    }

    /**
     * start work flow
     * <p>
     * select * from ACT_RU_EXECUTION;  -- execution record
     * select * from ACT_RU_TASK;       -- task record
     */
    @Test
    void startFlow() {
        String processId = "test_bpmn:1:8e3690d6-4af8-11ef-b160-ac91a14949e7";
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processId);
        log.info("start flow successfully! InstanceId = {}", processInstance.getId());
        // String processKey = "test_bpmn";
        // runtimeService.startProcessInstanceByKey(processKey); // key was defined by ourselves

    }


    /**
     * find flow task by assignee
     */
    @Test
    void findFlowByAssignee() {
        List<Task> list = taskService.createTaskQuery()
                .taskAssignee("wayne")
                .list();
        log.info("get {} task successfully!", "wayne");
        for (Task task : list) {
            log.info("task id = {}", task.getId());
        }
    }

    /**
     * same to start flow.
     * After completing task, will delete related data in 'ACT_RU_' table.
     */
    @Test
    void completeTask() {

        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee("wayne")
                .list();
        Task task = tasks.get(0);
        log.info("get {} task successfully! id = {}", "wayne", task.getId());
        taskService.complete(task.getId());
        log.info("complete task successfully! id = {}", task.getId());
    }
    
    @Test
    void deleteDeployFlow() {
        repositoryService.deleteDeployment("");
    }
}

