package com.fwd.fsm.flowable.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FlowTaskDTO {
	/** current task id */
	private String taskId;
	/** current task name */
	private String taskName;
	/** current user id */
	private String userId;
	/** task comment */
	private String comment;
	/** process instance id */
	private String instanceId;
	/** process instance node */
	private String targetKey;
	/** deployment id */
	private String deploymentId;
	/** process definition id */
	private String defId;
	/** child execution id */
	private String currentChildExecutionId;
	/**  Has the sub execution flow been executed flag */
	private Boolean flag;
	/** flow variables */
	private Map<String, Object> variables;
	/** flow assignee */
	private String assignee;
	/** flow candidate users */
	private List<String> candidateUsers;
	/** flow candidate groups */
	private List<String> candidateGroups;
}
