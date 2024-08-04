package com.fwd.fsm.flowable.controller;

import lombok.extern.slf4j.Slf4j;
import com.fwd.fsm.flowable.common.Response;
import com.fwd.fsm.flowable.domain.dto.FlowTaskDTO;
import com.fwd.fsm.flowable.service.flow.FlowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * flow task controller
 */
@Slf4j
@RestController
@RequestMapping("/task")
public class FlowTaskController {

    @Autowired
    FlowTaskService flowTaskService;

    @GetMapping(value = "/todo/list")
    public Response queryTodoTaskList(@RequestParam(value = "userId") String userId) {
        return Response.success(flowTaskService.queryTodoTaskList(userId));
    }

    @PostMapping(value = "/claim")
    public Response claim(@RequestParam(value = "userId") String userId,
                          @RequestParam(value = "taskId") String taskId) {
        return Response.success(flowTaskService.claim(userId, taskId));
    }

    @PostMapping(value = "/unClaim")
    public Response unClaim(@RequestParam(value = "taskId") String taskId) {
        return Response.success( flowTaskService.unClaim(taskId));
    }

    @PostMapping(value = "/complete")
    public Response complete(@RequestBody FlowTaskDTO flowTaskDTO) {
        flowTaskService.complete(flowTaskDTO);
        return Response.success();
    }

    @GetMapping(value = "/finished/list")
    public Response finishedTaskList(@RequestParam(value = "taskId") String taskId) {
        return Response.success(flowTaskService.finishedTaskList(taskId));
    }

    @PostMapping(value = "/rollback")
    public Response rollbackTask(@RequestBody FlowTaskDTO flowTaskDTO) {
        flowTaskService.rollbackTask(flowTaskDTO);
        return Response.success();
    }

    @PostMapping(value = "/reject")
    public Response rejectTask(@RequestBody FlowTaskDTO flowTaskDTO) {
        flowTaskService.rejectTask(flowTaskDTO);
        return Response.success();
    }
}
