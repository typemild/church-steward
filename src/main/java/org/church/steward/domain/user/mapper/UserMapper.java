package org.church.steward.domain.user.mapper;

import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .role(user.getRole())
                .remarks(user.getRemarks())
                .build();
    }
}
