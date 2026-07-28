package com.dreamoutbox.ams.user;

import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isEnabled())
                .build();
    }

    @Transactional
    public User createUser(String username, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // If creating a user with role TENANT, ensure a linked Tenant profile exists
        if (role == Role.TENANT && tenantRepository.findByUserId(savedUser.getId()).isEmpty()) {
            Tenant tenant = Tenant.builder()
                    .user(savedUser)
                    .fullName(fullName)
                    .phone("N/A")
                    .idCardNumber("ID-" + savedUser.getUsername())
                    .creditBalance(BigDecimal.ZERO)
                    .build();
            tenantRepository.save(tenant);
        }

        return savedUser;
    }

    @Transactional
    public User updateUser(UUID id, String fullName, Role role, Boolean enabled, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setFullName(fullName);
        user.setRole(role);
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        // Also update linked tenant name if applicable
        tenantRepository.findByUserId(user.getId()).ifPresent(t -> {
            t.setFullName(fullName);
            tenantRepository.save(t);
        });

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        tenantRepository.findByUserId(user.getId()).ifPresent(tenantRepository::delete);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtAsc();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRoleOrderByCreatedAtAsc(role);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}
