package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.mapper.UserMapper;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.SecurityHelper;
import vn.edu.learningai.security.service.RefreshTokenService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock(mockMaker = MockMakers.SUBCLASS) private SecurityHelper securityHelper;
    @Mock(mockMaker = MockMakers.SUBCLASS) private RefreshTokenService refreshTokenService;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, userMapper, securityHelper, refreshTokenService);
    }

    @Test
    void emailSearchNormalizesCaseAndPreservesPaginationAndSort() {
        User user = User.builder().id(7L).email("student@example.test").build();
        UserResponse response = UserResponse.builder().id("7").email(user.getEmail()).build();
        Pageable pageable = PageRequest.of(1, 10, Sort.by("email").ascending().and(Sort.by("id").descending()));
        when(userRepository.searchByEmail("%student@%", pageable))
            .thenReturn(new PageImpl<>(List.of(user), pageable, 11));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        PageResponse<UserResponse> result = service.getUsers("  STUDENT@  ", PageRequest.of(1, 10, Sort.by("email")));

        assertThat(result.getItems()).containsExactly(response);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(11);
    }

    @Test
    void emailSearchTreatsSqlWildcardsAsLiteralCharacters() {
        when(userRepository.searchByEmail(eq("%name!_!%!!%"), any(Pageable.class))).thenReturn(Page.empty());

        service.getUsers("name_%!", PageRequest.of(0, 10));

        verify(userRepository).searchByEmail(eq("%name!_!%!!%"), any(Pageable.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingEmailDoesNotFilterTheList(String email) {
        when(userRepository.searchByEmail(isNull(), any(Pageable.class))).thenReturn(Page.empty());

        service.getUsers(email, PageRequest.of(0, 10));

        verify(userRepository).searchByEmail(isNull(), any(Pageable.class));
    }

    @Test
    void excessiveEmailSearchIsRejectedBeforeQuerying() {
        assertThatThrownBy(() -> service.getUsers("x".repeat(255), PageRequest.of(0, 10)))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getDetails().get(0).getField()).isEqualTo("email"));

        verifyNoInteractions(userRepository);
    }
}
