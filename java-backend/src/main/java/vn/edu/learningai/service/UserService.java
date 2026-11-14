package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.dto.request.UpdateProfileRequest;
import vn.edu.learningai.dto.request.UpdateUserStatusRequest;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.mapper.UserMapper;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.SecurityHelper;
import vn.edu.learningai.security.service.RefreshTokenService;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityHelper securityHelper;
    private final RefreshTokenService refreshTokenService;

    public UserResponse getCurrentUser() {
        User user = securityHelper.getCurrentUser();
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = securityHelper.getCurrentUser();
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
            user = userRepository.save(user);
        }
        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        user.setIsActive(request.getIsActive());
        user = userRepository.save(user);
        if (!request.getIsActive()) {
            refreshTokenService.revokeAllForUser(userId);
        }
        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(String email, Pageable pageable) {
        String pattern = null;
        if (email != null && !email.isBlank()) {
            String keyword = email.trim().toLowerCase(Locale.ROOT);
            if (keyword.length() > 254) {
                throw ApiException.fieldError("email", "Email search must not exceed 254 characters");
            }
            pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        }
        Sort sort = normalizeSort(pageable.getSort());
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<User> users = userRepository.searchByEmail(pattern, request);
        Page<UserResponse> responses = users.map(user -> userMapper.toUserResponse(user));
        return PageResponse.from(responses);
    }

    private Sort normalizeSort(Sort requested) {
        if (requested.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt", "id");
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : requested) {
            String property = order.getProperty();
            if (property.equals("full_name") || property.equals("fullName")) {
                property = "fullName";
            } else if (property.equals("is_active") || property.equals("isActive")) {
                property = "isActive";
            } else if (property.equals("created_at") || property.equals("createdAt")) {
                property = "createdAt";
            } else if (!property.equals("id") && !property.equals("email") && !property.equals("role")) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported sort field");
            }
            orders.add(new Sort.Order(order.getDirection(), property));
        }
        Sort sort = Sort.by(orders);
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return sort;
    }
}
