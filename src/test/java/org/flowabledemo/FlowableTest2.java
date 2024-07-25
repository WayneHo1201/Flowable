package org.flowabledemo;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.junit.jupiter.api.Test;

class FlowableTest2 {

    @Test
    void deployFlow() {
        // obtain flowable engine config object
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                .setJdbcUrl("jdbc:sqlserver://10.22.88.200:48000;databaseName=FSM_TH_TEST2")
                .setJdbcUsername("svc_fsm")
                .setJdbcPassword("Password1");
        ProcessEngine processEngine = cfg.buildProcessEngine();
        System.out.println(processEngine);
    }
}
