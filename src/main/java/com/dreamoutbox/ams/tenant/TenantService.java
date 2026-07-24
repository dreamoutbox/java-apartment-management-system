package com.dreamoutbox.ams.tenant;

import com.dreamoutbox.ams.common.FileStorageService;
import com.dreamoutbox.ams.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public Tenant createTenant(String fullName, String phone, String email, String idCardNumber, MultipartFile idCardImage, User user) {
        String imagePath = null;
        if (idCardImage != null && !idCardImage.isEmpty()) {
            imagePath = fileStorageService.storeFile(idCardImage, "id-cards");
        }

        Tenant tenant = Tenant.builder()
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .idCardNumber(idCardNumber)
                .idCardImagePath(imagePath)
                .user(user)
                .build();

        return tenantRepository.save(tenant);
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

    @Transactional(readOnly = true)
    public Tenant getByUserId(UUID userId) {
        return tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant profile not found for user: " + userId));
    }
}
