package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.common.enums.TeacherRequestStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.CreateTeacherRequest;
import vn.edu.learningai.dto.request.TeacherDecisionRequest;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.response.TeacherRequestResponse;
import vn.edu.learningai.entity.TeacherRequest;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.TeacherRequestMapper;
import vn.edu.learningai.repository.TeacherRequestRepository;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.SecurityHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherRequestService {
    private final TeacherRequestRepository teacherRequestRepository;
    private final UserRepository userRepository;
    private final TeacherRequestMapper teacherRequestMapper;
    private final SecurityHelper securityHelper;

    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public TeacherRequestResponse create(CreateTeacherRequest request) {
        User user = securityHelper.getCurrentUser();

        if (teacherRequestRepository.existsByUserIdAndStatus(user.getId(), TeacherRequestStatus.PENDING)) {
            throw new ApiException(ErrorCode.INVALID_STATE, "A pending teacher request already exists");
        }

        TeacherRequest teacherRequest = TeacherRequest.builder()
            .user(user)
            .reason(request.getReason())
            .status(TeacherRequestStatus.PENDING)
            .build();

        return teacherRequestMapper.toResponse(teacherRequestRepository.save(teacherRequest));
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherRequestResponse> getMyRequests(Pageable pageable) {
        User user = securityHelper.getCurrentUser();
        Sort sort = normalizeSort(pageable.getSort());
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<TeacherRequest> teacherRequests = teacherRequestRepository.findByUserId(user.getId(), request);
        return PageResponse.from(teacherRequests.map(teacherRequest -> teacherRequestMapper.toResponse(teacherRequest)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<TeacherRequestResponse> getRequests(TeacherRequestStatus status, Pageable pageable) {
        securityHelper.getCurrentUser();
        Sort sort = normalizeSort(pageable.getSort());
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<TeacherRequest> teacherRequests;
        if (status == null) {
            teacherRequests = teacherRequestRepository.findAll(request);
        } else {
            teacherRequests = teacherRequestRepository.findByStatus(status, request);
        }
        return PageResponse.from(teacherRequests.map(teacherRequest -> teacherRequestMapper.toResponse(teacherRequest)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TeacherRequestResponse decide(Long requestId, TeacherDecisionRequest request) {
        if (request.getStatus() == null || request.getStatus() == TeacherRequestStatus.PENDING) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Decision status must be APPROVED or REJECTED");
        }
        User admin = securityHelper.getCurrentUser();
        TeacherRequest teacherRequest = teacherRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Teacher request not found"));
        Long userId = teacherRequest.getUser().getId();
        if (admin.getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You cannot decide your own teacher request");
        }
        if (teacherRequest.getStatus() != TeacherRequestStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Teacher request has already been decided");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        if (request.getStatus() == TeacherRequestStatus.APPROVED) {
            if (user.getRole() != UserRole.STUDENT || !Boolean.TRUE.equals(user.getIsActive())) {
                throw new ApiException(ErrorCode.INVALID_STATE, "Only an active student can become a teacher");
            }
            user.setRole(UserRole.TEACHER);
            userRepository.save(user);
        }

        teacherRequest.setStatus(request.getStatus());
        teacherRequest.setDecidedBy(admin);
        teacherRequest.setDecisionNote(request.getDecisionNote());
        teacherRequest.setDecidedAt(Instant.now());
        teacherRequest = teacherRequestRepository.save(teacherRequest);
        return teacherRequestMapper.toResponse(teacherRequest);
    }

    private Sort normalizeSort(Sort requested) {
        if (requested.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt", "id");
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : requested) {
            String property = order.getProperty();
            if (property.equals("created_at") || property.equals("createdAt")) {
                property = "createdAt";
            } else if (property.equals("decided_at") || property.equals("decidedAt")) {
                property = "decidedAt";
            } else if (!property.equals("id") && !property.equals("status")) {
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
