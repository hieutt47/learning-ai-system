package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.request.UpdateProfileRequest;
import vn.edu.learningai.dto.request.UpdateUserStatusRequest;
import vn.edu.learningai.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PatchMapping("/{user_id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
        @PathVariable("user_id") Long userId,
        @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(userId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
        @RequestParam(required = false) String email,
        Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(email, pageable));
    }
}
