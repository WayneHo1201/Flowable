package org.flowabledemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.flowabledemo.common.Response;
import org.flowabledemo.service.FlowableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/task")
public class FlowTaskController {

    @Autowired
    private FlowableService flowableService;

    @GetMapping(value = "/list")
    public Response startFlow (@RequestParam String name) {
        return Response.success(flowableService.getTaskListByName(name));
    }
}
