package com.kubee.auth.common.service;

import com.kubee.auth.user.entity.User;
import com.kubee.auth.user.entity.UserApplication;
import com.kubee.auth.user.entity.UserModulePrivilege;
import com.kubee.auth.user.repository.UserRepository;
import com.kubee.security.UserContextUtil;
import com.kubee.common.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may change tenant-wide settings (business details, addresses, integrations, subscription):
 * platform users, the tenant's SUPER_ADMIN, or anyone holding a *_SETTINGS_EDIT privilege in any app.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String SETTINGS_EDIT_SUFFIX = "_SETTINGS_EDIT";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean canEditSettings() {
        if (UserContextUtil.isPlatformUser()) {
            return true;
        }
        Long userId = UserContextUtil.getUserId();
        if (userId == null) {
            return false;
        }
        User caller = userRepository.findById(userId).orElse(null);
        if (caller == null) {
            return false;
        }
        boolean superAdmin = caller.getUserRoles() != null && caller.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getIsActive()) && SUPER_ADMIN_ROLE.equals(ur.getRole().getRoleKey()));
        if (superAdmin) {
            return true;
        }
        return caller.getUserApplications() != null && caller.getUserApplications().stream()
                .filter(UserApplication::getIsActive)
                .flatMap(ua -> ua.getModulePrivileges().stream())
                .filter(UserModulePrivilege::getIsActive)
                .anyMatch(ump -> ump.getPrivilege().getPrivilegeKey().endsWith(SETTINGS_EDIT_SUFFIX));
    }

    public void requireSettingsEditor() throws CommonException {
        if (!canEditSettings()) {
            throw new CommonException("You don't have permission to change business settings", HttpStatus.FORBIDDEN);
        }
    }
}
