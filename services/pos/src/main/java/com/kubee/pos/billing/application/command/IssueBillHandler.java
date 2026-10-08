package com.kubee.pos.billing.application.command;

import com.kubee.pos.billing.application.port.BillNumberPort;
import com.kubee.pos.billing.application.port.OrderSnapshotPort;
import com.kubee.pos.billing.application.port.OrderSnapshotPort.LineSnapshot;
import com.kubee.pos.billing.application.port.OrderSnapshotPort.OrderSnapshot;
import com.kubee.pos.billing.application.port.SellerPort;
import com.kubee.pos.billing.application.port.SellerPort.SellerSettings;
import com.kubee.pos.billing.application.port.TaxComponentPort;
import com.kubee.pos.billing.domain.Bill;
import com.kubee.pos.billing.domain.BillBuyer;
import com.kubee.pos.billing.domain.BillLineSpec;
import com.kubee.pos.billing.domain.BillNumber;
import com.kubee.pos.billing.domain.BillRepository;
import com.kubee.pos.billing.domain.BillSeller;
import com.kubee.pos.billing.domain.BillStatus;
import com.kubee.pos.billing.domain.BillTotals;
import com.kubee.pos.billing.domain.FinancialYear;
import com.kubee.pos.billing.domain.TaxComponentSpec;
import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.common.time.ShopTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IssueBillHandler implements CommandHandler<IssueBillCommand, String> {


    private final BillRepository billRepository;
    private final OrderSnapshotPort orders;
    private final SellerPort sellers;
    private final TaxComponentPort taxComponents;
    private final BillNumberPort billNumbers;

    @Override
    @Transactional
    public String handle(IssueBillCommand command) {
        OrderSnapshot order = orders.findOrder(command.orderUuid())
                .orElseThrow(() -> NotFoundException.of("Order", command.orderUuid()));
        Optional<Bill> existing = billRepository.findFirstByOrderIdAndStatus(order.id(), BillStatus.ISSUED);
        if (existing.isPresent()) {
            return sameBillOrConflict(existing.get(), command);
        }
        Guard.isTrue("COMPLETED".equals(order.status()),
                "Only a completed (fully paid) order can be billed (this one is " + order.status() + ")");
        Guard.isTrue(!"REFUNDED".equals(order.paymentStatus()), "This order was fully refunded; there is nothing to bill");
        SellerSettings seller = sellers.currentSeller()
                .orElseThrow(() -> new DomainException("Add the shop name and address in settings before issuing bills"));

        String series = BillNumber.normaliseSeries(command.series());
        FinancialYear year = FinancialYear.of(ShopTime.today(), seller.financialYearStartMonth());
        String number = BillNumber.format(seller.billPrefix(), series, year, billNumbers.next(series, year.key()));

        Map<Long, List<TaxComponentSpec>> components = taxComponents.componentsOf(order.lines().stream()
                .map(LineSnapshot::taxGroupId).filter(Objects::nonNull).distinct().toList());
        List<BillLineSpec> lines = order.lines().stream().map(line -> toSpec(line, components)).toList();

        var buyer = new BillBuyer(
                command.customerName() != null ? command.customerName() : order.customerName(),
                command.customerPhone() != null ? command.customerPhone() : order.customerPhone(),
                command.customerGstin(), command.placeOfSupply());
        var totals = new BillTotals(Money.of(order.subTotal()), Money.of(order.discountAmount()),
                Money.of(order.taxableAmount()), Money.of(order.taxAmount()), Money.of(order.roundOffAmount()),
                Money.of(order.grandTotal()));
        var shop = new BillSeller(seller.shopName(), seller.address(), seller.gstin(), seller.stateCode());

        Bill bill = Bill.issue(order.id(), number, shop, buyer, totals, lines,
                TenantContext.currentUserUuid().orElse(null));
        return billRepository.save(bill).getUuid();
    }

    /** A retry gets the same bill back; asking for different customer details needs a cancel first. */
    private static String sameBillOrConflict(Bill bill, IssueBillCommand command) {
        boolean differs = differs(command.customerGstin(), bill.getCustomerGstin())
                || differs(command.placeOfSupply(), bill.getPlaceOfSupply())
                || differs(command.customerName(), bill.getCustomerName())
                || differs(command.customerPhone(), bill.getCustomerPhone());
        if (differs) {
            throw new ConflictException("Order already has bill " + bill.getBillNumber()
                    + ". Cancel it to issue a new one with different customer details.");
        }
        return bill.getUuid();
    }

    private static boolean differs(String requested, String onBill) {
        String value = Guard.trimToNull(requested);
        return value != null && !value.replace(" ", "").replace("-", "").equalsIgnoreCase(onBill);
    }

    private static BillLineSpec toSpec(LineSnapshot l, Map<Long, List<TaxComponentSpec>> components) {
        String name = l.variantName() == null ? l.itemName() : l.itemName() + " (" + l.variantName() + ")";
        return new BillLineSpec(l.orderItemId(), l.itemId(), name, l.addonsText(), l.hsnSacCode(), l.unitOfMeasure(),
                l.quantity(), Money.of(l.unitPrice()), Money.of(l.lineAmount()), Money.of(l.discountAmount()),
                l.taxRate(), Money.of(l.taxableAmount()), Money.of(l.taxAmount()), Money.of(l.totalAmount()),
                l.taxGroupId() == null ? List.of() : components.getOrDefault(l.taxGroupId(), List.of()));
    }
}
