package com.fwd.fsm.flowable.service.flow;

import com.fwd.fsm.flowable.common.constant.FlowInstanceConstants;
import com.fwd.fsm.flowable.domain.vo.FlowInstanceVO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class FlowInstanceService {

    @Autowired
    RuntimeService runtimeService;
    @Autowired
    HistoryService historyService;

    public FlowInstanceVO startProcessInstanceById(String procDefId, Map<String, Object> variables) {
        FlowInstanceVO flowInstanceVO = null;
        try {
            // TODO set initiator info to variables, probably get from system request
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, variables);
            log.info("start process successfully! id = {}", procDefId);
            flowInstanceVO = convertInstance(processInstance);
        } catch (Exception e) {
            log.error("start process fail! id = {}", procDefId, e);
        }
        return flowInstanceVO;
    }

    public void updateState(Integer state, String instanceId) {
        // active
        if (Objects.equals(state, FlowInstanceConstants.ACTIVE)) {
            runtimeService.activateProcessInstanceById(instanceId);
        }
        // suspend
        if (Objects.equals(state, FlowInstanceConstants.SUSPEND)) {
            runtimeService.suspendProcessInstanceById(instanceId);
        }
    }

    /**
     * delete flow instance
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String instanceId, String deleteReason) {
        // query history data
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceById(instanceId);
        if (historicProcessInstance.getEndTime() != null) {
            historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
            return;
        }
        // delete process instance
        runtimeService.deleteProcessInstance(instanceId, deleteReason);
        // delete history instance
        historyService.deleteHistoricProcessInstance(instanceId);
    }

    public HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance =
                historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (Objects.isNull(historicProcessInstance)) {
            throw new FlowableObjectNotFoundException("flow instance does not exist, id = " + processInstanceId);
        }
        return historicProcessInstance;
    }


    private FlowInstanceVO convertInstance(ProcessInstance processInstance) {
        return FlowInstanceVO.builder()
                .instanceId(processInstance.getProcessInstanceId())
                .name(processInstance.getName())
                .flowKey(processInstance.getProcessDefinitionKey())
                .category(processInstance.getProcessDefinitionCategory())
                .version(processInstance.getProcessDefinitionVersion())
                .deploymentId(processInstance.getDeploymentId())
                .isSuspended(processInstance.isSuspended())
                .description(processInstance.getDescription())
                .startTime(processInstance.getStartTime())
                .build();
    }
}
