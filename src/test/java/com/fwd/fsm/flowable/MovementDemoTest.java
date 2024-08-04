package com.fwd.fsm.flowable;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class MovementDemoTest {
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
                .addClasspathResource("processes/MovementApproval.bpmn20.xml")
                .name("Movement Approval test demo")
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
        String processId = "movement_demo:3:6e558b32-4b3e-11ef-bc8f-ac91a14949e7";
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processId);
        log.info("start flow successfully! InstanceId = {}", processInstance.getId());
        // String processKey = "test_bpmn";
        // runtimeService.startProcessInstanceByKey(processKey); // key was defined by ourselves

    }


    /**
     * find flow task by assignee
     */
    @Test
    void findCandidateAndClaimTask() {
        // find group base on current user
        Group group = identityService.createGroupQuery().groupMember("Wayne").singleResult();
        log.info("Wayne groupId = {}", group.getId());
        List<Task> list = taskService.createTaskQuery().taskCandidateGroup(group.getId()).list();
        for (Task task : list) {
            log.info("task id = {}, name = {}", task.getId(), task.getName());
            taskService.claim(task.getId(), "Wayne");
        }
    }

    /**
     * same to start flow.
     * After completing task, will delete related data in 'ACT_RU_' table.
     */
    @Test
    void completeCommonTask() {

        Task task = taskService.createTaskQuery()
                .taskAssignee("wayne")
                .list().getFirst();
        // submit process
        log.info("Movement submit process");
        taskService.complete(task.getId());
        log.info("complete task successfully! id = {}", task.getId());
    }


    /**
     * find flow task by assignee
     */
    @Test
    void findManagerCandidateAndClaimTask() {
        // find group base on current user
        Group group = identityService.createGroupQuery().groupMember("Boger").singleResult();
        log.info("Boger groupId = {}", group.getId());
        List<Task> list = taskService.createTaskQuery().taskCandidateGroup(group.getId()).list();
        for (Task task : list) {
            log.info("task id = {}, name = {}", task.getId(), task.getName());
            taskService.claim(task.getId(), "Boger");
        }
    }

    /**
     * same to start flow.
     * After completing task, will delete related data in 'ACT_RU_' table.
     */
    @Test
    void completeManagerTask() {

        Task task = taskService.createTaskQuery()
                .taskAssignee("Boger")
                .list().getFirst();
        log.info("Movement approve process");
        taskService.complete(task.getId());
        log.info("complete task successfully! id = {}", task.getId());
    }




    @Test
    void deleteDeployFlow() {
        repositoryService.deleteDeployment("");
    }
}

