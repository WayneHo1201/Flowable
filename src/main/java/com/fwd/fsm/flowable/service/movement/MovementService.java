package com.fwd.fsm.flowable.service.movement;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * movement demo
 */
@Service
@Slf4j
public class MovementService {
	@Autowired
	TaskService taskService;
	public Double submit(DelegateExecution execution, String customValue) {
		Map<String, Object> variables = execution.getVariables();
		Double sum = (Double) variables.get("sum");
		String movementId = (String) variables.get("movementId");
		log.info("movement submit, start to handle logic");
		sum += 300d;
		log.info("movement id = {}, amount = {}, customValue = {}", movementId, sum, customValue);
		return sum;
	}

	public void approve(DelegateExecution execution) {
		Map<String, Object> variables = execution.getVariables();
		String movementId = (String) variables.get("movementId");
		Double amount = (Double) variables.get("Amount");
		log.info("movement approve, start to handle logic, movement id = {}, amount = {}", movementId, amount);
	}
}
