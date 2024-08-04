package com.fwd.fsm.flowable.util;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.util.*;


@Slf4j
public class FlowableUtils {
	private FlowableUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static List<SequenceFlow> getElementIncomingFlows(FlowElement source) {
		List<SequenceFlow> sequenceFlows = null;
		if (source instanceof FlowNode flowNode) {
			sequenceFlows = flowNode.getIncomingFlows();
		} else if (source instanceof Gateway gateway) {
			sequenceFlows = gateway.getIncomingFlows();
		} else if (source instanceof SubProcess subProcess) {
			sequenceFlows = subProcess.getIncomingFlows();
		} else if (source instanceof StartEvent startEvent) {
			sequenceFlows = startEvent.getIncomingFlows();
		} else if (source instanceof EndEvent endEvent) {
			sequenceFlows = endEvent.getIncomingFlows();
		}
		return sequenceFlows;
	}

	public static List<SequenceFlow> getElementOutgoingFlows(FlowElement source) {
		List<SequenceFlow> sequenceFlows = null;
		if (source instanceof FlowNode flowNode) {
			sequenceFlows = flowNode.getOutgoingFlows();
		} else if (source instanceof Gateway gateway) {
			sequenceFlows = gateway.getOutgoingFlows();
		} else if (source instanceof SubProcess subProcess) {
			sequenceFlows = subProcess.getOutgoingFlows();
		} else if (source instanceof StartEvent startEvent) {
			sequenceFlows = startEvent.getOutgoingFlows();
		} else if (source instanceof EndEvent endEvent) {
			sequenceFlows = endEvent.getOutgoingFlows();
		}
		return sequenceFlows;
	}

	public static Collection<FlowElement> getAllElements(Collection<FlowElement> flowElements, Collection<FlowElement> allElements) {
		allElements = allElements == null ? new ArrayList<>() : allElements;

		for (FlowElement flowElement : flowElements) {
			allElements.add(flowElement);
			if (flowElement instanceof SubProcess subprocess) {
				allElements = FlowableUtils.getAllElements(subprocess.getFlowElements(), allElements);
			}
		}
		return allElements;
	}
	/**
	 * Iterate to obtain the list of parent task nodes and search forward
	 *
	 * @param source          starting node of param source
	 * @param hasSequenceFlow used to determine if the line is duplicated
	 * @param userTaskList    List of user tasks that need to be recalled
	 * @return
	 */
	public static List<UserTask> iteratorFindParentUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
		userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

		if (source instanceof StartEvent && source.getSubProcess() != null) {
			userTaskList = iteratorFindParentUserTasks(source.getSubProcess(), hasSequenceFlow, userTaskList);
		}

		List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

		if (sequenceFlows != null) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				if (sequenceFlow.getSourceFlowElement() instanceof UserTask userTask) {
					userTaskList.add(userTask);
					continue;
				}
				if (sequenceFlow.getSourceFlowElement() instanceof SubProcess) {
					List<UserTask> childUserTaskList = findChildProcessUserTasks((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, null);
					if (childUserTaskList != null && childUserTaskList.isEmpty()) {
						userTaskList.addAll(childUserTaskList);
						continue;
					}
				}
				userTaskList = iteratorFindParentUserTasks(sequenceFlow.getSourceFlowElement(), hasSequenceFlow, userTaskList);
			}
		}
		return userTaskList;
	}

	/**
	 * Find the way from back to front and obtain all the points on the dirty circuit
	 *
	 * @param source          starting node of param source
     * @param runTaskKeyList  The running task key is used to verify whether the task node is a running node
	 * @param hasSequenceFlow used to determine if the line is duplicated
	 * @param userTaskList    List of user tasks that need to be recalled
	 * @return
	 */
	public static List<UserTask> iteratorFindChildUserTasks(FlowElement source, List<String> runTaskKeyList, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
		userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;

		if (source instanceof EndEvent && source.getSubProcess() != null) {
			userTaskList = iteratorFindChildUserTasks(source.getSubProcess(), runTaskKeyList, hasSequenceFlow, userTaskList);
		}

		List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

		if (sequenceFlows != null) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				if (sequenceFlow.getTargetFlowElement() instanceof UserTask userTask && runTaskKeyList.contains((sequenceFlow.getTargetFlowElement()).getId())) {
					userTaskList.add(userTask);
					continue;
				}
				if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
					List<UserTask> childUserTaskList = iteratorFindChildUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), runTaskKeyList, hasSequenceFlow, null);
					if (childUserTaskList != null && !childUserTaskList.isEmpty()) {
						userTaskList.addAll(childUserTaskList);
						continue;
					}
				}
				userTaskList = iteratorFindChildUserTasks(sequenceFlow.getTargetFlowElement(), runTaskKeyList, hasSequenceFlow, userTaskList);
			}
		}
		return userTaskList;
	}

	/**
	 * Iteratively obtain sub process user task nodes
	 *
	 * @param source          starting node of param source
	 * @param hasSequenceFlow used to determine if the line is duplicated
	 * @return
	 */
	public static List<UserTask> findChildProcessUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
		userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;

		List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

		if (sequenceFlows != null) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				if (sequenceFlow.getTargetFlowElement() instanceof UserTask userTask) {
					userTaskList.add(userTask);
					continue;
				}
				if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
					List<UserTask> childUserTaskList = findChildProcessUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, null);
					if (childUserTaskList != null && !childUserTaskList.isEmpty()) {
						userTaskList.addAll(childUserTaskList);
						continue;
					}
				}
				userTaskList = findChildProcessUserTasks(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, userTaskList);
			}
		}
		return userTaskList;
	}

	/**
	 * Find the way from back to front and obtain all the points on the dirty circuit
	 *
	 * @param source          starting node of param source
	 * @param passRoads       The set of points that have already been passed by parampassRoads
	 * @param hasSequenceFlow used to determine if the line is duplicated
	 * @param targets         targets target dirty route endpoint
	 * @param dirtyRoads      The parameter dirtyRoads is identified as a dirty data point, and since it does not need to be duplicated, it is stored using a set
	 * @return
	 */
	public static Set<String> iteratorFindDirtyRoads(FlowElement source, List<String> passRoads, Set<String> hasSequenceFlow, List<String> targets, Set<String> dirtyRoads) {
		passRoads = passRoads == null ? new ArrayList<>() : passRoads;
		dirtyRoads = dirtyRoads == null ? new HashSet<>() : dirtyRoads;
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

		if (source instanceof StartEvent && source.getSubProcess() != null) {
			dirtyRoads = iteratorFindDirtyRoads(source.getSubProcess(), passRoads, hasSequenceFlow, targets, dirtyRoads);
		}

		List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

		if (sequenceFlows != null) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				passRoads.add(sequenceFlow.getSourceFlowElement().getId());
				if (targets.contains(sequenceFlow.getSourceFlowElement().getId())) {
					dirtyRoads.addAll(passRoads);
					continue;
				}
				if (sequenceFlow.getSourceFlowElement() instanceof SubProcess) {
					dirtyRoads = findChildProcessAllDirtyRoad((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, dirtyRoads);
					Boolean isInChildProcess = dirtyTargetInChildProcess((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, targets, null);
					if (isInChildProcess) {
						continue;
					}
				}
				dirtyRoads = iteratorFindDirtyRoads(sequenceFlow.getSourceFlowElement(), passRoads, hasSequenceFlow, targets, dirtyRoads);
			}
		}
		return dirtyRoads;
	}

	/**
	 * Iteratively obtain dirty routes for subprocesses
	 * Explanation: If the point of rollback is the subprocess, then it will definitely roll back to the initial user task node of the subprocess, so all nodes in the subprocess are dirty routes
	 *
	 * @param source  node of param source
	 * @param hasSequenceFlow  ID of the connection that has already passed through the paramhasSequenceFlow, used to determine if the line is duplicated
	 * @param dirtyRoads parameter dirtyRoads is identified as a dirty data point, and since it does not need to be duplicated, it is stored using a set
	 * @return
	 */
	public static Set<String> findChildProcessAllDirtyRoad(FlowElement source, Set<String> hasSequenceFlow, Set<String> dirtyRoads) {
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
		dirtyRoads = dirtyRoads == null ? new HashSet<>() : dirtyRoads;

		List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

		if (sequenceFlows != null) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				dirtyRoads.add(sequenceFlow.getTargetFlowElement().getId());
				if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
					dirtyRoads = findChildProcessAllDirtyRoad((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, dirtyRoads);
				}
				dirtyRoads = findChildProcessAllDirtyRoad(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, dirtyRoads);
			}
		}
		return dirtyRoads;
	}

	/**
	 * Determine whether the end node of the dirty route is on the subprocess
	 *
	 * @param source node of param source
	 * @param hasSequenceFlow The ID of the connection that has already passed through the paramhasSequenceFlow, used to determine if the line is duplicated
	 * @param targets  parameter targets determine whether there are any sub processes on the dirty route nodes. As long as there is one, it means that the dirty route only ends at the sub process明脏路线只到子流程为止
	 * @param inChildProcess  paraminChildProcess exist on a subprocess? True is yes, false is no
	 * @return
	 */
	public static Boolean dirtyTargetInChildProcess(FlowElement source, Set<String> hasSequenceFlow, List<String> targets, Boolean inChildProcess) {
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
		inChildProcess = inChildProcess != null && inChildProcess;

		List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

		if (sequenceFlows != null && !inChildProcess) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				if (targets.contains(sequenceFlow.getTargetFlowElement().getId())) {
					inChildProcess = true;
					break;
				}
				if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
					inChildProcess = dirtyTargetInChildProcess((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, targets, inChildProcess);
				}
				inChildProcess = dirtyTargetInChildProcess(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, targets, inChildProcess);
			}
		}
		return inChildProcess;
	}

	/**
	 * Iteration scans from back to front to determine whether the target node is serial relative to the current node
	 * There is no situation of directly falling back into the subprocess, but there is a situation of going out from the subprocess to the parent process
	 *
	 * @param source node of param source
	 * @param targetKsy
	 * @param isSequential the parameter isSequential serial
	 * @param hasSequenceFlow ID of the connection that has already passed through the paramhasSequenceFlow, used to determine if the line is duplicated
	 * @return
	 */
	public static Boolean iteratorCheckSequentialReferTarget(FlowElement source, String targetKsy, Set<String> hasSequenceFlow, Boolean isSequential) {
		isSequential = isSequential == null || isSequential;
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

		if (source instanceof StartEvent && source.getSubProcess() != null) {
			isSequential = iteratorCheckSequentialReferTarget(source.getSubProcess(), targetKsy, hasSequenceFlow, isSequential);
		}

		List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

		if (sequenceFlows != null) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				if (!isSequential) {
					break;
				}
				if (targetKsy.equals(sequenceFlow.getSourceFlowElement().getId())) {
					continue;
				}
				if (sequenceFlow.getSourceFlowElement() instanceof StartEvent) {
					isSequential = false;
					break;
				}
				isSequential = iteratorCheckSequentialReferTarget(sequenceFlow.getSourceFlowElement(), targetKsy, hasSequenceFlow, isSequential);
			}
		}
		return isSequential;
	}

	/**
	 * Find the way from back to front and obtain all routes to reach the node
	 * There is no direct rollback to the child process, but there is a situation of rollback to the parent process
	 *
	 * @param source
	 * @param passRoads of param source
	 * @param hasSequenceFlow of points that have already been passed by parampassRoads
	 * @param roads route
	 * @return
	 */
	public static List<List<UserTask>> findRoad(FlowElement source, List<UserTask> passRoads, Set<String> hasSequenceFlow, List<List<UserTask>> roads) {
		passRoads = passRoads == null ? new ArrayList<>() : passRoads;
		roads = roads == null ? new ArrayList<>() : roads;
		hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

		if (source instanceof StartEvent && source.getSubProcess() != null) {
			roads = findRoad(source.getSubProcess(), passRoads, hasSequenceFlow, roads);
		}

		List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

		if (sequenceFlows != null && !sequenceFlows.isEmpty()) {
			for (SequenceFlow sequenceFlow : sequenceFlows) {
				if (hasSequenceFlow.contains(sequenceFlow.getId())) {
					continue;
				}
				hasSequenceFlow.add(sequenceFlow.getId());
				if (sequenceFlow.getSourceFlowElement() instanceof UserTask userTask) {
					passRoads.add(userTask);
				}
				roads = findRoad(sequenceFlow.getSourceFlowElement(), passRoads, hasSequenceFlow, roads);
			}
		} else {
			roads.add(passRoads);
		}
		return roads;
	}

	/**
	 * Cleaning historical node data, removing dirty data caused by rollback after cleaning
	 *
	 * @param allElements              all elements info
	 * @param historicTaskInstanceList Historical task instance information, data in ascending order of start time
	 * @return
	 */
	public static List<String> historicTaskInstanceClean(Collection<FlowElement> allElements, List<HistoricTaskInstance> historicTaskInstanceList) {
		List<String> multiTask = new ArrayList<>();
		allElements.forEach(flowElement -> {
			if (flowElement instanceof UserTask userTask) {
				if (userTask.getBehavior() instanceof ParallelMultiInstanceBehavior || ((UserTask) flowElement).getBehavior() instanceof SequentialMultiInstanceBehavior) {
					multiTask.add(flowElement.getId());
				}
			}
		});
		Stack<HistoricTaskInstance> stack = new Stack<>();
		historicTaskInstanceList.forEach(stack::push);
		List<String> lastHistoricTaskInstanceList = new ArrayList<>();
		StringBuilder userTaskKey = null;
		List<String> deleteKeyList = new ArrayList<>();
		List<Set<String>> dirtyDataLineList = new ArrayList<>();
		int multiIndex = -1;
		StringBuilder multiKey = null;
		boolean multiOpera = false;
		while (!stack.empty()) {
			final boolean[] isDirtyData = {false};
			for (Set<String> oldDirtyDataLine : dirtyDataLineList) {
				if (oldDirtyDataLine.contains(stack.peek().getTaskDefinitionKey())) {
					isDirtyData[0] = true;
				}
			}
			if (stack.peek().getDeleteReason() != null && !"MI_END".equals(stack.peek().getDeleteReason())) {
				String dirtyPoint = "";
				if (stack.peek().getDeleteReason().contains("Change activity to ")) {
					dirtyPoint = stack.peek().getDeleteReason().replace("Change activity to ", "");
				}
				if (stack.peek().getDeleteReason().contains("Change parent activity to ")) {
					dirtyPoint = stack.peek().getDeleteReason().replace("Change parent activity to ", "");
				}
				FlowElement dirtyTask = null;
				for (FlowElement flowElement : allElements) {
					if (flowElement.getId().equals(stack.peek().getTaskDefinitionKey())) {
						dirtyTask = flowElement;
					}
				}
				Set<String> dirtyDataLine = FlowableUtils.iteratorFindDirtyRoads(dirtyTask, null, null, Arrays.asList(dirtyPoint.split(",")), null);
				dirtyDataLine.add(stack.peek().getTaskDefinitionKey());
				log.info(stack.peek().getTaskDefinitionKey() + "dirty data collections: " + dirtyDataLine);
				boolean isNewDirtyData = true;
				for (int i = 0; i < dirtyDataLineList.size(); i++) {
					if (dirtyDataLineList.get(i).contains(userTaskKey.toString())) {
						isNewDirtyData = false;
						dirtyDataLineList.get(i).addAll(dirtyDataLine);
					}
				}
				if (isNewDirtyData) {
					deleteKeyList.add(dirtyPoint + ",");
					dirtyDataLineList.add(dirtyDataLine);
				}
				isDirtyData[0] = true;
			}
			if (!isDirtyData[0]) {
				lastHistoricTaskInstanceList.add(stack.peek().getTaskDefinitionKey());
			}
			for (int i = 0; i < deleteKeyList.size(); i++) {
				if (multiKey == null && multiTask.contains(stack.peek().getTaskDefinitionKey())
						&& deleteKeyList.get(i).contains(stack.peek().getTaskDefinitionKey())) {
					multiIndex = i;
					multiKey = new StringBuilder(stack.peek().getTaskDefinitionKey());
				}
				if (multiKey != null && !multiKey.toString().equals(stack.peek().getTaskDefinitionKey())) {
					deleteKeyList.set(multiIndex, deleteKeyList.get(multiIndex).replace(stack.peek().getTaskDefinitionKey() + ",", ""));
					multiKey = null;
					multiOpera = true;
				}
				if (multiKey == null && deleteKeyList.get(i).contains(stack.peek().getTaskDefinitionKey())) {
					deleteKeyList.set(i, deleteKeyList.get(i).replace(stack.peek().getTaskDefinitionKey() + ",", ""));
				}
				if ("".equals(deleteKeyList.get(i))) {
					deleteKeyList.remove(i);
					dirtyDataLineList.remove(i);
					break;
				}
			}
			if (multiOpera && deleteKeyList.size() > multiIndex && "".equals(deleteKeyList.get(multiIndex))) {
				deleteKeyList.remove(multiIndex);
				dirtyDataLineList.remove(multiIndex);
				multiIndex = -1;
				multiOpera = false;
			}
			userTaskKey = new StringBuilder(stack.pop().getTaskDefinitionKey());
		}
		log.info("Cleaned historical node data: " + lastHistoricTaskInstanceList);
		return lastHistoricTaskInstanceList;
	}
}
