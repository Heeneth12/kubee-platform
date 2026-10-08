package com.kubee.pos.billing.infrastructure.persistence;

import com.kubee.pos.billing.application.port.BillNumberPort;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bill counter in pos.number_sequences (type BILL, per series and financial year). The upsert locks the
 * row until commit, and a failed bill rolls the counter back, so numbers stay gap-free as GST expects.
 */
@Component
@RequiredArgsConstructor
class JdbcBillNumberGenerator implements BillNumberPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public long next(String series, String financialYearKey) {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid())
                .addValue("uuid", UUID.randomUUID().toString())
                .addValue("series", series)
                .addValue("period", financialYearKey);
        Long next = jdbc.queryForObject("""
                INSERT INTO pos.number_sequences AS s (uuid, tenant_uuid, sequence_type, series_code, period_key, current_value)
                VALUES (:uuid, :tenant, 'BILL', :series, :period, 1)
                ON CONFLICT (tenant_uuid, sequence_type, series_code, period_key)
                DO UPDATE SET current_value = s.current_value + 1, updated_at = CURRENT_TIMESTAMP
                RETURNING current_value
                """, params, Long.class);
        return next == null ? 1 : next;
    }
}
