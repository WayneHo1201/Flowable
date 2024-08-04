package com.fwd.fsm.flowable.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * flow process instance
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlowInstanceVO implements Serializable {

    private String instanceId;

    private String name;

    private String flowKey;

    private String category;

    private int version;

    private String deploymentId;

    private Boolean isSuspended;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
}
