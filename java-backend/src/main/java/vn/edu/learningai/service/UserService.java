package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.mapper.UserMapper;
import vn.edu.learningai.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserResponse> getUsers()
    {
        return userMapper.toListUserResponse(userRepository.findAll());
    }
}
