package com.kubee.pos.reporting.application.query;

import java.time.YearMonth;

/** Read-only report queries over the order, payment and bill tables of the current shop. */
public interface ReportQueryRepository {

    SalesSummaryReport salesSummary(ReportPeriod period);

    PaymentModeReport paymentModes(ReportPeriod period);

    ItemSalesReport itemSales(ItemSalesQuery query);

    GstReport gstSummary(ReportPeriod period);

    CancellationReport cancellations(ReportPeriod period);

    HourlySalesReport hourlySales(ReportPeriod period);

    StaffSalesReport staffSales(ReportPeriod period);

    CategorySalesReport categorySales(ReportPeriod period);

    ChannelSalesReport channelSales(ReportPeriod period);

    ShiftReport shifts(ReportPeriod period);

    /** B2C invoices to another state above {@link Gstr1Builder#B2CL_LIMIT} are listed invoice-wise (B2CL). */
    Gstr1Data gstr1(YearMonth month);
}
