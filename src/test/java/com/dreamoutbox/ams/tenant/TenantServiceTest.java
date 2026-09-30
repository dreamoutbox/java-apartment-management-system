package com.dreamoutbox.ams.tenant;

import com.dreamoutbox.ams.common.FileStorageService;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserRepository;
import com.dreamoutbox.ams.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TenantService tenantService;

    private User user;
    private Tenant tenant;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        user = User.builder()
                .username("tenant1")
                .fullName("Tenant One")
                .role(Role.TENANT)
                .enabled(true)
                .build();

        tenant = Tenant.builder()
                .user(user)
                .fullName("Tenant One")
                .phone("0812345678")
                .idCardNumber("ID-123")
                .build();
    }

    @Test
    void testDeleteTenantSoftDeletesTenantAndUser() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        tenantService.deleteTenant(tenantId);

        assertTrue(tenant.isDeleted());
        assertNotNull(tenant.getDeletedAt());
        assertTrue(user.isDeleted());
        assertFalse(user.isEnabled());
        assertNotNull(user.getDeletedAt());

        verify(tenantRepository).save(tenant);
        verify(userRepository).save(user);
        verify(tenantRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
    }
}
