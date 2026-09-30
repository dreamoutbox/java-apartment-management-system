package com.dreamoutbox.ams.user;

import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private Tenant tenant;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .username("testuser")
                .fullName("Test User")
                .role(Role.TENANT)
                .enabled(true)
                .build();
        user.setId(userId);

        tenant = Tenant.builder()
                .user(user)
                .fullName("Test User")
                .build();
    }

    @Test
    void testDeleteUserSoftDeletesUserAndTenant() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantRepository.findByUserId(userId)).thenReturn(Optional.of(tenant));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        userService.deleteUser(userId);

        assertTrue(user.isDeleted());
        assertFalse(user.isEnabled());
        assertNotNull(user.getDeletedAt());
        assertTrue(tenant.isDeleted());
        assertNotNull(tenant.getDeletedAt());

        verify(userRepository).save(user);
        verify(tenantRepository).save(tenant);
        verify(userRepository, never()).delete(any());
        verify(tenantRepository, never()).delete(any());
    }

    @Test
    void testLoadUserByUsernameRejectsDeletedUser() {
        user.setDeleted(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("testuser"));
    }
}
