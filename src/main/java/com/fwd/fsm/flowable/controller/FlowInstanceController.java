package com.fwd.fsm.flowable.controller;

import com.fwd.fsm.flowable.common.Response;
import com.fwd.fsm.flowable.service.flow.FlowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * Flow instance controller
 */
@Slf4j
@RestController
@RequestMapping("/instance")
public class FlowInstanceController {

    @Autowired
    FlowInstanceService flowInstanceService;

    @PostMapping(value = "/start/{id}")
    public Response startFlow(@PathVariable(value = "id") String procDefId,
                              @RequestBody Map<String, Object> variables) {
        return Response.success(flowInstanceService.startProcessInstanceById(procDefId, variables));
    }

    /**
     * @param state 1 = active   2 = suspend
     */
    @PostMapping(value = "/update_state")
    public Response updateState(@RequestParam(value = "state") Integer state,
                                @RequestParam(value = "instanceId") String instanceId) {
        flowInstanceService.updateState(state, instanceId);
        return Response.success(instanceId);
    }


    @PostMapping(value = "/delete/{instanceIds}")
    public Response stopProcessInstance(@PathVariable(value = "instanceIds") String[] instanceIds,
                                        @RequestParam(value = "deleteReason", required = false) String deleteReason) {
        for (String instanceId : instanceIds) {
            flowInstanceService.delete(instanceId, deleteReason);
        }
        return Response.success(instanceIds);
    }

}
