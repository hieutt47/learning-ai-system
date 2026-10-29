package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import vn.edu.learningai.dto.request.RegisterRequest;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.entity.User;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UserMapper {

    public UserResponse toUserResponse(User user);
    public List<UserResponse> toListUserResponse(List<User> users);
    public User toUser(RegisterRequest request);

}
