package com.dreamoutbox.ams.tenant;

import com.dreamoutbox.ams.common.FileStorageService;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserRepository;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Transactional
    public Tenant createTenantWithAccount(String username, String rawPassword, String fullName, String phone,
            String email, String idCardNumber, MultipartFile idCardImage) {
        User user = userService.createUser(username, rawPassword, fullName, Role.TENANT);

        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElse(Tenant.builder().user(user).creditBalance(BigDecimal.ZERO).build());

        tenant.setFullName(fullName);
        tenant.setPhone(phone != null ? phone : "N/A");
        tenant.setEmail(email);
        tenant.setIdCardNumber(idCardNumber != null ? idCardNumber : "ID-" + username);

        if (idCardImage != null && !idCardImage.isEmpty()) {
            String imagePath = fileStorageService.storeFile(idCardImage, "id-cards");
            tenant.setIdCardImagePath(imagePath);
        }

        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateTenant(UUID id, String fullName, String phone, String email, String idCardNumber,
            MultipartFile idCardImage) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        tenant.setFullName(fullName);
        tenant.setPhone(phone);
        tenant.setEmail(email);
        tenant.setIdCardNumber(idCardNumber);

        if (tenant.getUser() != null) {
            tenant.getUser().setFullName(fullName);
            userRepository.save(tenant.getUser());
        }

        if (idCardImage != null && !idCardImage.isEmpty()) {
            String imagePath = fileStorageService.storeFile(idCardImage, "id-cards");
            tenant.setIdCardImagePath(imagePath);
        }

        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        User user = tenant.getUser();
        tenantRepository.delete(tenant);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant getById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    @Transactional
    public Tenant getByUserId(UUID userId) {
        return tenantRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && user.getRole() == Role.TENANT) {
                        Tenant t = Tenant.builder()
                                .user(user)
                                .fullName(user.getFullName())
                                .phone("N/A")
                                .idCardNumber("ID-" + user.getUsername())
                                .creditBalance(BigDecimal.ZERO)
                                .build();
                        return tenantRepository.save(t);
                    }
                    throw new IllegalArgumentException("Tenant profile not found for user: " + userId);
                });
    }
}
