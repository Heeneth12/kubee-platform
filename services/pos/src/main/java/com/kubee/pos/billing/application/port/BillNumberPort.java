package com.kubee.pos.billing.application.port;

/** Gap-free running bill counter per series and financial year. */
public interface BillNumberPort {

    /** Next value for (series, financial year key). Runs in the caller's transaction. */
    long next(String series, String financialYearKey);
}
