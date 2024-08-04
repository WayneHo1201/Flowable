package com.fwd.fsm.flowable;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
class FlowableTest2 {
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
    @Test
    void deployFlow() {
        // obtain flowable engine config object
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                .setJdbcUrl("jdbc:sqlserver://10.22.88.200:48000;databaseName=FSM_TH_TEST2")
                .setJdbcUsername("svc_fsm")
                .setJdbcPassword("Password1");
        ProcessEngine processEngine = cfg.buildProcessEngine();
        System.out.println(processEngine);
    }


    @Test
    void generate() {
        //开始事件
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        startEvent.setName("开始");
        //审批节点
        UserTask userTask = new UserTask();
        userTask.setId("CATEGORYID_FILE_AUDIT");
        userTask.setCategory("SEAL_AUDIT1");
        userTask.setCandidateUsers(Arrays.asList("2422042842433986573"));
        userTask.setName("审批节点1");
        //添加任务节点Listener

        //permissinTaskListener 为实现TaskListener的一个类的bean名称

        //非spring下可以这样用

        //   listener.setImplementationType("class");
        //  listener.setImplementation("实现类的全限定名");
        List<FlowableListener> taskListeners = new ArrayList<>();
        FlowableListener listener = new FlowableListener();
        listener.setEvent("all");
        listener.setImplementationType("delegateExpression");
        listener.setImplementation("${permissinTaskListener}");
        //listener.setId("CATEGORYID_FILE_AUDIT_listener");
        taskListeners.add(listener);
        userTask.setTaskListeners(taskListeners);
        //
        ExclusiveGateway fileGateway = new ExclusiveGateway();
        fileGateway.setId("FILE_GATEWAY");

        UserTask userTask2 = new UserTask();
        userTask2.setId("CATEGORYID_SEAL_AUDIT");
        userTask2.setCategory("SEAL_AUDIT2");
        userTask2.setCandidateUsers(Arrays.asList("2198741168097853440"));
        userTask2.setName("审批节点2");

        ExclusiveGateway sealGateway = new ExclusiveGateway();
        sealGateway.setId("SEAL_GATEWAY");
        //Service Task ，sealAuthDelegate 为实现JavaDelegate的spring bean 名称
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId("SEAL_AUTH");
        serviceTask.setName("自动授权节点");
        serviceTask.setImplementationType("delegateExpression");
        serviceTask.setImplementation("${sealAuthDelegate}");

        //节点
        UserTask usesealTask = new UserTask();
        usesealTask.setCandidateUsers(Arrays.asList("${sealUsers}"));
        usesealTask.setId("CATEGORYID_SEAL_USE");
        usesealTask.setCategory("SEAL_USE1");
        usesealTask.setName("使用节点");
        //拒绝
        EndEvent rejectEvent = new EndEvent();
        rejectEvent.setId("reject");
        rejectEvent.setName("拒绝");
        //结束事件
        EndEvent completeEvent = new EndEvent();
        completeEvent.setId("complete");
        completeEvent.setName("结束");

        SequenceFlow sequenceFlow = new SequenceFlow(startEvent.getId(), userTask.getId());
        SequenceFlow sequenceFlow2 = new SequenceFlow(userTask.getId(), fileGateway.getId());
        SequenceFlow sequenceFlow3 = new SequenceFlow(fileGateway.getId(), userTask2.getId());
        sequenceFlow3.setConditionExpression("${approve}");
        SequenceFlow sequenceFlow4 = new SequenceFlow(fileGateway.getId(), rejectEvent.getId());
        sequenceFlow4.setConditionExpression("${!approve}");
        SequenceFlow sequenceFlow5 = new SequenceFlow(userTask2.getId(), sealGateway.getId());
        SequenceFlow sequenceFlow6 = new SequenceFlow(sealGateway.getId(), serviceTask.getId());
        sequenceFlow6.setConditionExpression("${approve}");
        SequenceFlow sequenceFlow7 = new SequenceFlow(sealGateway.getId(), rejectEvent.getId());
        sequenceFlow7.setConditionExpression("${approve}");
        SequenceFlow sequenceFlow8 = new SequenceFlow(serviceTask.getId(), usesealTask.getId());
        SequenceFlow sequenceFlow9 = new SequenceFlow(usesealTask.getId(), completeEvent.getId());

        /*
         * 整合节点和连线成为一个 process
         */
        org.flowable.bpmn.model.Process process = new Process();
        process.setId("CATAGORY_PHYSIC_SEAL_");
        process.setName("业务防伪用印分类流程");
        process.addFlowElement(startEvent);
        process.addFlowElement(userTask);
        process.addFlowElement(fileGateway);
        process.addFlowElement(userTask2);
        process.addFlowElement(sealGateway);
        process.addFlowElement(serviceTask);
        process.addFlowElement(usesealTask);
        process.addFlowElement(rejectEvent);
        process.addFlowElement(completeEvent);
        process.addFlowElement(sequenceFlow);
        process.addFlowElement(sequenceFlow2);
        process.addFlowElement(sequenceFlow3);
        process.addFlowElement(sequenceFlow4);
        process.addFlowElement(sequenceFlow5);
        process.addFlowElement(sequenceFlow6);
        process.addFlowElement(sequenceFlow7);
        process.addFlowElement(sequenceFlow8);
        process.addFlowElement(sequenceFlow9);
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        //new BpmnAutoLayout(bpmnModel).execute();
//        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
//        ProcessValidator defaultProcessValidator = processValidatorFactory.createDefaultProcessValidator();
//        // 验证失败信息的封装ValidationError
//        List<ValidationError> validate = defaultProcessValidator.validate(bpmnModel);
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] convertToXML = bpmnXMLConverter.convertToXML(bpmnModel);
        String bytes = new String(convertToXML);
        System.out.println(bytes);
        String tenantId = "2422042838839468037";
        Deployment deploy = repositoryService.createDeployment().tenantId(tenantId).addString("分类流程.bpmn", bytes).deploy();
        log.info("======部署id:" + deploy.getId());
    }
}
