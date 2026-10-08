package com.kubee.pos.ordering.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.ordering.application.port.OrderSettingsPort;
import com.kubee.pos.ordering.domain.RoundOffMode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Reads pos.pos_settings directly until the shop/settings module exists. */
@Component
@RequiredArgsConstructor
class JdbcOrderSettings implements OrderSettingsPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public RoundOffMode roundOffMode() {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid());
        return jdbc.queryForList("""
                        SELECT round_off_mode FROM pos.pos_settings
                        WHERE tenant_uuid = :tenant AND is_deleted = false
                        """, params, String.class).stream()
                .findFirst()
                .map(RoundOffMode::valueOf)
                .orElse(RoundOffMode.NEAREST_1);
    }
}
