package org.flowabledemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.flowabledemo.common.Response;
import org.flowabledemo.service.FlowableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/definition")
public class FlowDefinitionController  {

    @Autowired
    private FlowableService flowableService;

    @GetMapping(value = "/get")
    public Response getFlow (@RequestParam String key,
                         @RequestParam(required = false) int version) {
        return Response.success(flowableService.getProcdef(key, version).getId());
    }
}
