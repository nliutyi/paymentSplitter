package com.eleks.groupservice.mapper;

import com.eleks.groupservice.domain.Group;
import com.eleks.groupservice.dto.GroupRequestDto;
import com.eleks.groupservice.dto.GroupResponseDto;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class GroupMapper {

    public static Group toEntity(GroupRequestDto groupRequestDto) {
        return ofNullable(groupRequestDto)
                .map(dto -> Group.builder()
                        .groupName(dto.getGroupName())
                        .currency(dto.getCurrency())
                        .members(dto.getMembers())
                        .build())
                .orElse(null);
    }

    public static GroupResponseDto toDto(Group groupEntity) {
        return ofNullable(groupEntity)
                .map(entity -> GroupResponseDto.builder()
                        .id(entity.getId())
                        .groupName(entity.getGroupName())
                        .currency(entity.getCurrency())
                        .members(entity.getMembers())
                        .build())
                .orElse(null);
    }

}
