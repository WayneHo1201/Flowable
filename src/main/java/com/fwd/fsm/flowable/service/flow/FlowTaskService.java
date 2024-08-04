package com.fwd.fsm.flowable.service.flow;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import com.fwd.fsm.flowable.common.constant.FlowTaskConstants;
import com.fwd.fsm.flowable.domain.dto.FlowTaskDTO;
import com.fwd.fsm.flowable.domain.vo.FlowTaskVO;
import com.fwd.fsm.flowable.exception.CustomFlowException;
import com.fwd.fsm.flowable.util.FlowableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class FlowTaskService {

	@Autowired
	TaskService taskService;
	@Autowired
	RepositoryService repositoryService;
	@Autowired
	HistoryService historyService;
	@Autowired
	RuntimeService runtimeService;

	/**
	 * query list by user id
	 */
	public List<FlowTaskVO> queryTodoTaskList(String userId) {
		List<Task> taskList = taskService.createTaskQuery()
				.active()
				.includeCaseVariables()
				.taskCandidateOrAssigned(userId)
				.orderByTaskCreateTime().desc().list();
		return convertTaskList(taskList);
	}

	/**
	 * query list by group list
	 */
	public List<FlowTaskVO> queryTodoTaskList(List<String> groupList) {
		List<Task> taskList = taskService.createTaskQuery()
				.active()
				.includeCaseVariables()
				.taskCandidateGroupIn(groupList)
				.orderByTaskCreateTime().desc().list();
		return convertTaskList(taskList);
	}


	private List<FlowTaskVO> convertTaskList(List<Task> taskList) {
		return taskList.stream().map(this::convertTask).toList();
	}

	/**
	 * convert processDefinition to vo
	 */
	private FlowTaskVO convertTask(Task task) {
		return FlowTaskVO.builder()
				.taskId(task.getId())
				.parentTaskId(task.getParentTaskId())
				.processDefinitionId(task.getProcessDefinitionId())
				.processInstanceId(task.getProcessInstanceId())
				.taskDefinitionKey(task.getTaskDefinitionKey())
				.name(task.getName())
				.category(task.getCategory())
				.description(task.getDescription())
				.claimTime(task.getClaimTime())
				.createTime(task.getCreateTime())
				.build();
	}

	/**
	 * query flow task by taskId
	 */
	private FlowTaskVO queryFlowTaskByTaskId(String taskId) {
		return convertTask(taskService.createTaskQuery().active().taskId(taskId).singleResult());
	}

	/**
	 * pick up task
	 */
	public FlowTaskVO claim(String userId, String taskId) {
		taskService.claim(taskId, userId);
		return queryFlowTaskByTaskId(taskId);
	}

	/**
	 * put task back to pool
	 */
	public FlowTaskVO unClaim(String taskId) {
		taskService.unclaim(taskId);
		return queryFlowTaskByTaskId(taskId);

	}

	@Transactional(rollbackFor = Exception.class)
	public void complete(FlowTaskDTO flowTaskDTO) {
		String taskId = flowTaskDTO.getTaskId();
		String userId = flowTaskDTO.getUserId();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (Objects.isNull(task)) {
			throw new FlowableObjectNotFoundException("Cannot find task! id = " + taskId);
		}
		FlowTaskVO flowTask = queryTodoTaskList(userId).stream().filter(flowTaskVO -> taskId.equals(flowTaskVO.getTaskId())).findAny().orElse(null);
		if (Objects.isNull(flowTask)) {
			throw new FlowableObjectNotFoundException("This task is not in your task list. Cannot be completed. userId = " + userId + " taskId = " + taskId);
		}
		// Both of comment and process none blank can add comment
		if (StringUtils.isNoneBlank(flowTaskDTO.getComment(), flowTaskDTO.getInstanceId())) {
			taskService.addComment(taskId, flowTaskDTO.getInstanceId(), FlowTaskConstants.APPROVE, flowTaskDTO.getComment());
			log.debug("Add comment for task {}", taskId);
		}
		taskService.complete(taskId, flowTaskDTO.getVariables());
	}


	public List<FlowTaskVO> finishedTaskList(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			throw new FlowableObjectNotFoundException("cannot find task! id = " + taskId);
		}
		String taskDefinitionKey = task.getTaskDefinitionKey();
		log.info("current task definition key is {}", taskDefinitionKey);
		BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		Process mainProcess = bpmnModel.getMainProcess();
		// current node
		FlowNode currentFlowElement = (FlowNode) mainProcess.getFlowElement(taskDefinitionKey, true);
		// query history node
		List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId(task.getProcessInstanceId())
				.finished()
				.orderByHistoricActivityInstanceEndTime()
				.asc().list();
		List<String> activityIdList = activityInstanceList.stream()
				.filter(activityInstance ->
						BpmnXMLConstants.ELEMENT_TASK_USER.equals(activityInstance.getActivityType()) || BpmnXMLConstants.ELEMENT_EVENT_START.equals(activityInstance.getActivityType()))
				.map(HistoricActivityInstance::getActivityId)
				.filter(activityId -> !taskDefinitionKey.equals(activityId))
				.distinct()
				.toList();
		List<FlowTaskVO> voList = new ArrayList<>();
		for (String activityId : activityIdList) {
			// roll back to main process
			FlowNode toBackFlowElement = (FlowNode) mainProcess.getFlowElement(activityId, true);
			// find list of source node to current node
			Set<String> set = new HashSet<>();
			if (toBackFlowElement != null && ExecutionGraphUtil.isReachable(mainProcess, toBackFlowElement, currentFlowElement, set)) {
				voList.add(FlowTaskVO.builder()
						.taskDefinitionKey(activityId)
						.name(toBackFlowElement.getName())
						.build());
			}
		}
		return voList;
	}

	/**
	 * roll back flow task to specific node
	 */
	@Transactional(rollbackFor = Exception.class)
	public void rollbackTask(FlowTaskDTO flowTaskDTO) {
		if (taskService.createTaskQuery().taskId(flowTaskDTO.getTaskId()).singleResult().isSuspended()) {
			throw new CustomFlowException("task is suspended");
		}
		// current task
		Task task = taskService.createTaskQuery().taskId(flowTaskDTO.getTaskId()).singleResult();
		// obtain process definition
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
		// obtain process
		Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().getFirst();
		// obtain all nodes including child nodes
		Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
		FlowElement source = null;
		FlowElement target = null;
		if (allElements != null) {
			for (FlowElement flowElement : allElements) {
				//  current node element
				if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
					source = flowElement;
				}
				// target node element
				if (flowElement.getId().equals(flowTaskDTO.getTargetKey())) {
					target = flowElement;
				}
			}
		}
		if (target == null) {
			throw new FlowableObjectNotFoundException("Cannot find target flow element, definition key = " + flowTaskDTO.getTargetKey());
		}
		Boolean isSequential = FlowableUtils.iteratorCheckSequentialReferTarget(source, flowTaskDTO.getTargetKey(), null, null);
		if (Boolean.FALSE.equals(isSequential)) {
			throw new CustomFlowException("The current node is not in a serial relationship with the target node and cannot be rolled back");
		}
		//Obtain the key of all normal task nodes, which cannot be directly used. It is necessary to identify the tasks that need to be revoked
		List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
		List<String> runTaskKeyList = new ArrayList<>();
		runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
		// task list
		List<String> currentIds = new ArrayList<>();
		//Obtain the tasks that need to be recalled by comparing them with runTaskList through the exit connection of the parent gateway
		List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null, null);
		currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

		List<String> currentTaskIds = new ArrayList<>();
		currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
			if (currentId.equals(runTask.getTaskDefinitionKey())) {
				currentTaskIds.add(runTask.getId());
			}
		}));
		// setting roll back comment
		currentTaskIds.forEach(currentTaskId -> taskService.addComment(currentTaskId, task.getProcessInstanceId(), FlowTaskConstants.ROLL_BACK, flowTaskDTO.getComment()));

		try {
			runtimeService.createChangeActivityStateBuilder()
					.processInstanceId(task.getProcessInstanceId())
					.moveActivityIdsToSingleActivityId(currentIds, flowTaskDTO.getTargetKey()).changeState();
			log.info("Roll back task from {} to {}", task.getName(), target.getName());
		} catch (FlowableObjectNotFoundException e) {
			throw new CustomFlowException("Process instance not found, process may have changed");
		} catch (FlowableException e) {
			throw new CustomFlowException("Cannot cancel or start activity");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void rejectTask(FlowTaskDTO flowTaskDTO) {
		if (taskService.createTaskQuery().taskId(flowTaskDTO.getTaskId()).singleResult().isSuspended()) {
			throw new CustomFlowException("task is suspended");
		}
		// current task
		Task task = taskService.createTaskQuery().taskId(flowTaskDTO.getTaskId()).singleResult();
		// obtain process definition
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
		// obtain first node information
		Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().getFirst();

		// obtain a complete list of nodes, including child nodes
		Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
		// obtain the current task node element
		FlowElement source = null;
		if (allElements != null) {
			for (FlowElement flowElement : allElements) {
				// user node type
				if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
					// obtain node info
					source = flowElement;
				}
			}
		}

		/*
		 * Objective: To obtain all targetIds nodes that jump to
		 * Retrieve all parent user task nodes of the current node
		 * Depth first algorithm idea
		 */
		List<UserTask> parentUserTaskList = FlowableUtils.iteratorFindParentUserTasks(source, null, null);
		if (parentUserTaskList == null || parentUserTaskList.isEmpty()) {
			throw new CustomFlowException("The current node is the initial task node and cannot be rejected");
		}
		List<String> parentUserTaskKeyList = new ArrayList<>();
		parentUserTaskList.forEach(item -> parentUserTaskKeyList.add(item.getId()));
		// Obtain all historical node activity instances, that is, the node history that has been passed, and the data is in ascending order of start time
		List<HistoricTaskInstance> historicTaskInstanceList = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).orderByHistoricTaskInstanceStartTime().asc().list();
		// cleaning out dirty data caused by rollback
		List<String> lastHistoricTaskInstanceList = FlowableUtils.historicTaskInstanceClean(allElements, historicTaskInstanceList);
		// at this point, the historical task instance is in reverse order, and the last node to be accessed is obtained
		List<String> targetIds = new ArrayList<>();
		int number = 0;
		StringBuilder parentHistoricTaskKey = new StringBuilder();
		for (String historicTaskInstanceKey : lastHistoricTaskInstanceList) {
			// When signing, there may be a special situation where the historical data of the same node is continuously the same, which can be skipped
			if (parentHistoricTaskKey.toString().equals(historicTaskInstanceKey)) {
				continue;
			}
			parentHistoricTaskKey = new StringBuilder(historicTaskInstanceKey);
			if (historicTaskInstanceKey.equals(task.getTaskDefinitionKey())) {
				number++;
			}
			/* After data cleaning, the historical node is the only historical record from the beginning to the current node, and theoretically each point will only appear once
			 * If a loop occurs in the process, the point in the middle of each loop will only appear once, and the next occurrence will be the next loop
			 * Number==1, encountering the current node for the first time
			 * Number==2, encountered for the second time, representing the range of the last loop
			 */
			if (number == 2) {
				break;
			}
			// If the current historical node belongs to the parent node,
			// it means that the last time it passed through this point, and it needs to be returned to this point
			if (parentUserTaskKeyList.contains(historicTaskInstanceKey)) {
				targetIds.add(historicTaskInstanceKey);
			}
		}
		//To obtain the current Ids of all nodes that need to be redirected
		//Take one of the parent tasks, as there will either be a common gateway or a serial common line in the future
		UserTask oneUserTask = parentUserTaskList.getFirst();
		// Obtain the key of all normal task nodes, which cannot be directly used. It is necessary to identify the tasks that need to be revoked
		List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
		List<String> runTaskKeyList = new ArrayList<>();
		runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
		// The task list needs to be rejected
		List<String> currentIds = new ArrayList<>();
		// Obtain the tasks that need to be recalled by comparing them with runTaskList through the exit connection of the parent gateway
		List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(oneUserTask, runTaskKeyList, null, null);
		currentUserTaskList.forEach(item -> currentIds.add(item.getId()));


		// Regulation: Before parallel gateway, there must be a unique user task node in the node.
		// If there are multiple task nodes, the parallel gateway node defaults to the end node because the many-to-many situation is not considered
		if (targetIds.size() > 1 && currentIds.size() > 1) {
			throw new CustomFlowException("Task encounters many-to-many situations, cannot be recalled");
		}

		// Retrieve the IDs of the nodes that need to be revoked in a loop to set the reason for rejection
		List<String> currentTaskIds = new ArrayList<>();
		currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
			if (currentId.equals(runTask.getTaskDefinitionKey())) {
				currentTaskIds.add(runTask.getId());
			}
		}));
		// setting reject comment
		currentTaskIds.forEach(item -> taskService.addComment(item, task.getProcessInstanceId(), FlowTaskConstants.REJECT, flowTaskDTO.getComment()));

		try {
			// If there are more than one parent tasks,
			// it means that the current node is not a parallel node,
			// because the many-to-many situation is not considered
			if (targetIds.size() > 1) {
				//1 to multi task jump, current Ids current node (1), targetIds jumps to multiple nodes (multiple)
				runtimeService.createChangeActivityStateBuilder()
						.processInstanceId(task.getProcessInstanceId()).
						moveSingleActivityIdToActivityIds(currentIds.getFirst(), targetIds).changeState();
			}
			//If there is only one parent task, the current task may be a task in the gateway
			if (targetIds.size() == 1) {
				runtimeService.createChangeActivityStateBuilder()
						.processInstanceId(task.getProcessInstanceId())
						.moveActivityIdsToSingleActivityId(currentIds, targetIds.getFirst()).changeState();
			}
		} catch (FlowableObjectNotFoundException e) {
			throw new FlowableObjectNotFoundException("process instance not found, process may have changed");
		} catch (FlowableException e) {
			throw new FlowableException("cannot cancel or start activity");
		}

	}

}
