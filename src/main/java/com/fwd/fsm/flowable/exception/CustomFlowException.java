package com.fwd.fsm.flowable.exception;

import lombok.Getter;

import java.io.Serial;

@Getter
public class CustomFlowException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer code;

	private String message;

	public CustomFlowException(String message) {
		this.message = message;
	}

	public CustomFlowException(String message, Integer code) {
		this.message = message;
		this.code = code;
	}

	public CustomFlowException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}

}
