package com.fwd.fsm.flowable.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;


@Data
@Builder
public class FlowTaskVO {

    private String taskId;

    private String name;

    private String category;

    private String parentTaskId;

    private String processDefinitionId;

    private String taskDefinitionKey;

    private String processInstanceId;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date claimTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;


}
