package com.fwd.fsm.flowable.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.fwd.fsm.flowable.common.Response;
import com.fwd.fsm.flowable.service.flow.FlowDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Flow definition controller
 */
@Slf4j
@RestController
@RequestMapping("/definition")
public class FlowDefinitionController {

	@Autowired
	FlowDefinitionService flowDefinitionService;

	@GetMapping(value = "/query")
	public Response getFlow(@RequestParam(value = "key") String key,
	                        @RequestParam(value = "version", required = false) Integer version) {
		return Response.success(flowDefinitionService.getProcDef(key, version));
	}


	@GetMapping(value = "/list")
	public Response getFlowList(@RequestParam(value = "key") String key) {
		return Response.success(flowDefinitionService.getProcDefList(key));
	}

	@PostMapping(value = "/deploy")
	public Response deployFlow(@RequestParam(value = "name", required = false) String name,
	                           @RequestParam(value = "category", required = false) String category,
	                           @RequestParam(value = "tenantId", required = false) String tenantId,
	                           @RequestParam(value = "file") MultipartFile file) {
		return Response.success(flowDefinitionService.deployFlow(name, category, tenantId, file));
	}


	@GetMapping(value = "/read/xml/{processInstanceId}")
	public Response readXml(@PathVariable(value = "processInstanceId") String processInstanceId) {
		return Response.success(flowDefinitionService.readXml(processInstanceId));
	}

	@GetMapping(value = "/read/image/{processInstanceId}")
	public void readImage(@PathVariable(value = "processInstanceId") String processInstanceId,
	                      HttpServletResponse response) {
		flowDefinitionService.readImage(processInstanceId, response);
	}

	@DeleteMapping(value = "/{deployId}")
	public Response deleteDeployment(@PathVariable(value = "deployId") String deployId) {
		return Response.success(flowDefinitionService.deleteDeploymentById(deployId));
	}
}
