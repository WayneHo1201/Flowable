package com.fwd.fsm.flowable.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * flow process definition
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlowProcDefVO implements Serializable {

    private String processDefinitionId;

    private String name;

    private String flowKey;

    private String category;

    private int version;

    private String deploymentId;

    private int suspensionState;

    private String description;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date deploymentTime;


}
