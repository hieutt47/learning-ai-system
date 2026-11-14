package vn.edu.learningai.security.custom;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.entity.User;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private Long id;
    private String fullName;
    private String email;
    private String passwordHash;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean isActive;
    private boolean emailVerified;

    private CustomUserDetails(
        Long id,
        String fullName,
        String email,
        String passwordHash,
        Collection<? extends GrantedAuthority> authorities,
        Boolean isActive) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.isActive = isActive;
    }

    public static CustomUserDetails build(User user) {
        List<GrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        CustomUserDetails details = new CustomUserDetails(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPasswordHash(),
            authorities,
            user.getIsActive());
        details.emailVerified = user.getEmailVerifiedAt() != null;
        return details;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public UserRole getRole() {
        return UserRole.valueOf(authorities.iterator().next().getAuthority().substring("ROLE_".length()));
    }
}
