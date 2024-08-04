package com.fwd.fsm.flowable.service.flow;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import com.fwd.fsm.flowable.domain.dto.UserGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class FlowUserService {
	@Autowired
	IdentityService identityService;

	/**
	 * create new user and group
	 */
	public void create(List<UserGroupDTO> userGroupDTOList) {
		for (UserGroupDTO userGroupDTO : userGroupDTOList) {
			String userId = userGroupDTO.getUserId();
			String groupId = userGroupDTO.getGroupId();
			User user = identityService.createUserQuery().userId(userId).singleResult();
			if (!Objects.isNull(user)) {
				throw new FlowableIllegalArgumentException("user already exist. userId = " + userId);
			}
			user = identityService.newUser(userId);
			identityService.saveUser(user);
			log.info("create a new user. Name = {}", userId);
			Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
			// if group is null, create a new one
			if (Objects.isNull(group)) {
				group = identityService.newGroup(groupId);
				group.setName(userGroupDTO.getGroupName());
				group.setType(userGroupDTO.getGroupType());
				identityService.saveGroup(group);
				log.info("create group successfully! groupId = {}", groupId);
			}
			identityService.createMembership(userId, groupId);
			log.info("create membership successfully, userid = {}, groupId = {}", userId, groupId);
		}

	}
}
