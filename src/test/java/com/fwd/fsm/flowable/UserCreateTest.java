package com.fwd.fsm.flowable;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class UserCreateTest {

    @Autowired
    private IdentityService identityService;

    /**
     * create user
     *
     * select * from ACT_ID_USER;
     */
    @Test
    void createUser() {
        User user = identityService.newUser("wayne");
        user.setFirstName("he");
        user.setLastName("wayne");
        user.setEmail("wayneho1201@163.com");
        identityService.saveUser(user);
        log.info("create a new user. Name = {}", "wayne");
    }

    @Test
    void createManager() {
        User user = identityService.newUser("Boger");
        identityService.saveUser(user);
        log.info("create a new user. Name = {}", "Boger");
    }

    /**
     * create a group
     *
     * select * from ACT_ID_GROUP;
     */
    @Test
    void createGroup() {
        Group group1 = identityService.newGroup("AGCY");
        group1.setName("agency department");
        group1.setType("type1");
        identityService.saveGroup(group1);
        log.info("create group1 successfully! groupId = {}", group1.getId());

        Group group2 = identityService.newGroup("MG");
        group2.setName("manager");
        group2.setType("type2");
        identityService.saveGroup(group2);
        log.info("create group1 successfully! groupId = {}", group2.getId());



    }

    /**
     * create membership
     *
     * select * from ACT_ID_MEMBERSHIP;
     */
    @Test
    void createMemberShip() {
        Group group1 = identityService.createGroupQuery().groupId("AGCY").singleResult();
        Group group2 = identityService.createGroupQuery().groupId("MG").singleResult();

        List<User> list = identityService.createUserQuery().list();
        for (User user : list) {
            String id = user.getId();
            if ("Wayne".equals(id)) {
                identityService.createMembership(id, group1.getId());
                log.info("create membership successfully, userid = {}, groupId = {}", id, group1.getId());
            }
            if ("Boger".equals(id)) {
                identityService.createMembership(id, group2.getId());
                log.info("create membership successfully, userid = {}, groupId = {}", id, group2.getId());
            }
        }
    }
}

