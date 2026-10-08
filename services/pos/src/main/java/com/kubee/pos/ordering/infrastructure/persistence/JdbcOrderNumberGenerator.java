package com.kubee.pos.ordering.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.ordering.application.port.OrderNumberPort;
import com.kubee.pos.common.time.ShopTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Daily order tokens from pos.number_sequences (type ORDER, series A, period = today's date in India).
 * The upsert locks the counter row until the transaction ends, so two tills never get the same token.
 */
@Component
@RequiredArgsConstructor
class JdbcOrderNumberGenerator implements OrderNumberPort {


    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public String nextOrderNumber() {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid())
                .addValue("uuid", UUID.randomUUID().toString())
                .addValue("period", ShopTime.today().toString());
        Long next = jdbc.queryForObject("""
                INSERT INTO pos.number_sequences AS s (uuid, tenant_uuid, sequence_type, series_code, period_key, current_value)
                VALUES (:uuid, :tenant, 'ORDER', 'A', :period, 1)
                ON CONFLICT (tenant_uuid, sequence_type, series_code, period_key)
                DO UPDATE SET current_value = s.current_value + 1, updated_at = CURRENT_TIMESTAMP
                RETURNING current_value
                """, params, Long.class);
        return String.valueOf(next);
    }
}
