package org.flowabledemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.flowabledemo.common.Response;
import org.flowabledemo.service.FlowableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/process")
public class FlowProcessController {

    @Autowired
    private FlowableService flowableService;

    @GetMapping(value = "/start")
    public Response startFlow (@RequestParam String id) {
        return Response.success(flowableService.startFlow(id));
    }
}
