package com.fwd.fsm.flowable.controller;

import lombok.extern.slf4j.Slf4j;
import com.fwd.fsm.flowable.common.Response;
import com.fwd.fsm.flowable.domain.dto.UserGroupDTO;
import com.fwd.fsm.flowable.service.flow.FlowUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * flow user controller
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class FlowUserController {

    @Autowired
    FlowUserService flowUserService;

    @PostMapping(value = "/create")
    public Response complete(@RequestBody List<UserGroupDTO> userGroupDTOList) {
        flowUserService.create(userGroupDTOList);
        return Response.success();
    }

}
