package com.kubee.auth.config;

import com.kubee.auth.tenant.entity.BusinessType;
import com.kubee.auth.tenant.entity.Tenant;
import com.kubee.auth.tenant.entity.TenantDetails;
import com.kubee.auth.tenant.repository.TenantRepository;
import com.kubee.auth.user.entity.AccountScope;
import com.kubee.auth.user.entity.User;
import com.kubee.auth.user.entity.UserType;
import com.kubee.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first PLATFORM (Kubee owner) account on startup from environment variables.
 * Does nothing when the variables are unset or a PLATFORM user already exists.
 * If the email belongs to an existing user, that user is promoted instead (password untouched).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final String HQ_TENANT_CODE = "KUBEE-HQ";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${platform.admin.email:}")
    private String adminEmail;

    @Value("${platform.admin.password:}")
    private String adminPassword;

    @Value("${platform.admin.name:Kubee Admin}")
    private String adminName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()) {
            return;
        }
        if (userRepository.existsByAccountScope(AccountScope.PLATFORM)) {
            log.info("Platform admin already exists, skipping bootstrap");
            return;
        }

        User existing = userRepository.findByEmail(adminEmail).orElse(null);
        if (existing != null) {
            existing.setAccountScope(AccountScope.PLATFORM);
            existing.setUserType(UserType.KUBEE_OPS);
            userRepository.save(existing);
            log.warn("Promoted existing user {} to PLATFORM admin", adminEmail);
            return;
        }

        if (adminPassword.isBlank()) {
            log.warn("platform.admin.email is set but platform.admin.password is empty, skipping bootstrap");
            return;
        }

        Tenant hq = tenantRepository.findByTenantCode(HQ_TENANT_CODE)
                .orElseGet(this::createHqTenant);

        User admin = userRepository.save(User.builder()
                .fullName(adminName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .userType(UserType.KUBEE_OPS)
                .accountScope(AccountScope.PLATFORM)
                .isActive(true)
                .tenant(hq)
                .build());

        hq.setTenantAdmin(admin);
        tenantRepository.save(hq);

        log.warn("Created PLATFORM admin {} in tenant {}", adminEmail, HQ_TENANT_CODE);
    }

    private Tenant createHqTenant() {
        Tenant hq = Tenant.builder()
                .tenantName("Kubee HQ")
                .tenantCode(HQ_TENANT_CODE)
                .isPersonal(false)
                .isActive(true)
                .isVerify(true)
                .build();
        // Tenant DTO mapping expects details to exist
        hq.setTenantDetails(TenantDetails.builder()
                .tenant(hq)
                .businessType(BusinessType.SERVICE_PROVIDER)
                .legalName("Kubee HQ")
                .baseCurrency("INR")
                .timeZone("Asia/Kolkata")
                .build());
        return tenantRepository.save(hq);
    }
}
