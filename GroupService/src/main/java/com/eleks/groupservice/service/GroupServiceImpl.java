package com.eleks.groupservice.service;

import com.eleks.groupservice.client.UserClient;
import com.eleks.groupservice.domain.Group;
import com.eleks.groupservice.dto.GroupRequestDto;
import com.eleks.groupservice.dto.GroupResponseDto;
import com.eleks.groupservice.dto.UserDto;
import com.eleks.groupservice.dto.StatusResponseDto;
import com.eleks.groupservice.exception.ResourceNotFoundException;
import com.eleks.groupservice.exception.UsersIdsValidationException;
import com.eleks.groupservice.mapper.GroupMapper;
import com.eleks.groupservice.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.eleks.groupservice.service.PaymentsCalculationHelper.calculateValues;
import static java.util.stream.Collectors.toList;

@Service
public class GroupServiceImpl implements GroupService {

    private GroupRepository repository;
    private UserClient client;

    @Autowired
    public GroupServiceImpl(GroupRepository repository, UserClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Override
    public GroupResponseDto saveGroup(GroupRequestDto group) throws UsersIdsValidationException {
        if (!client.areUserIdsValid(group.getMembers())) {
            throw new UsersIdsValidationException("Few users don`t exist");
        }
        Group entity = GroupMapper.toEntity(group);
        Group savedEntity = repository.save(entity);
        return GroupMapper.toDto(savedEntity);
    }

    @Override
    public Optional<GroupResponseDto> getGroup(Long id) {
        return repository.findById(id).map(GroupMapper::toDto);
    }

    @Override
    public GroupResponseDto editGroup(Long id, GroupRequestDto requestDto) throws ResourceNotFoundException, UsersIdsValidationException {
        if (!repository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Group does't exist");
        }
        if (!client.areUserIdsValid(requestDto.getMembers())) {
            throw new UsersIdsValidationException("Group contains non existing users");
        }

        Group group = GroupMapper.toEntity(requestDto);
        group.setId(id);
        return GroupMapper.toDto(repository.save(group));
    }

    @Override
    public void deleteGroupById(Long id) throws ResourceNotFoundException {
        if (repository.findById(id).isPresent()) {
            repository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Group does't exist");
        }
    }

    @Override
    public List<StatusResponseDto> getGroupMembersStatus(Long groupId, Long requesterId) throws ResourceNotFoundException, UsersIdsValidationException {
        Group group = repository.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group doesn't exist"));

        if (!group.getMembers().contains(requesterId)) {
            throw new UsersIdsValidationException("User is not a member of the group");
        }

        List<Long> otherMembersIdsFromGroup = group.getMembers()
                .stream()
                .filter(memberId -> !memberId.equals(requesterId))
                .collect(toList());

        List<UserDto> members = client.getListOfUsersByIds(otherMembersIdsFromGroup);
        List<Long> realMembersIds = members.stream()
                .map(UserDto::getId)
                .collect(toList());
        Map<Long, Double> values = calculateValues(requesterId, group.getPayments(), realMembersIds);

        return members.stream()
                .map(member ->
                        StatusResponseDto.builder()
                                .userId(member.getId())
                                .username(member.getUsername())
                                .currency(group.getCurrency())
                                .value(values.get(member.getId()))
                                .build()
                ).collect(toList());
    }
}
