package phattrienungdungvoij2ee.bai5_qlsp_jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.*;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.ApartmentFeeTypeRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.ApartmentBillDetailRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.ApartmentMonthlyBillRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.PaymentRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ApartmentInvoiceService {

    @Autowired
    private ApartmentFeeTypeRepository feeTypeRepository;

    @Autowired
    private ApartmentMonthlyBillRepository billRepository;

    @Autowired
    private ApartmentBillDetailRepository billDetailRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    private static final BigDecimal RATE_YELLOW = new BigDecimal("0.0005");
    private static final BigDecimal RATE_RED = new BigDecimal("0.001");

    public List<InvoiceView> getInvoicesForUser(Account user) {
        if (user == null) {
            return new ArrayList<>();
        }
        ensureMonthlyBills(user);

        List<ApartmentMonthlyBill> bills = billRepository.findByAccountIdOrderByMonthKeyDesc(user.getId());
        List<InvoiceView> views = new ArrayList<>();
        for (ApartmentMonthlyBill b : bills) {
            ensureBillDetails(b, user, parseMonth(b.getMonthKey()));
            views.add(toView(b, user));
        }
        views.sort(Comparator.comparing((InvoiceView x) -> x.monthKey).reversed());
        return views;
    }

    public List<AdminInvoiceView> getInvoicesForAdminManager(List<Account> users) {
        List<AdminInvoiceView> rows = new ArrayList<>();
        if (users == null) {
            return rows;
        }
        for (Account u : users) {
            if (u == null || u.getChungCu() == null) {
                continue;
            }
            if (!isRole(u, "ROLE_USER")) {
                continue;
            }
            
            // 1. Lấy hóa đơn chung cư (Tổng hợp)
            List<InvoiceView> invoices = getInvoicesForUser(u);
            for (InvoiceView inv : invoices) {
                rows.add(new AdminInvoiceView(
                        "CHUNG_CU", "Phí tổng hợp căn hộ",
                        u.getLogin_name(),
                        u.getRoom(),
                        (u.getChungCu().getMaChungCu() != null ? u.getChungCu().getMaChungCu() : String.valueOf(u.getChungCu().getId())),
                        u.getChungCu().getName(),
                        inv.monthDisplay,
                        inv.status,
                        inv.totalToPay
                ));
            }
            
            // 2. Lấy Các thanh toán Dịch vụ riêng lẻ
            List<Payment> servicesPayments = paymentRepository.findBySubscriptionUserId(u.getId());
            if (servicesPayments != null) {
                for (Payment p : servicesPayments) {
                    if (p.getSubscription() == null) continue;
                    
                    String sname = "Dịch vụ (Không rõ)";
                    if (p.getSubscription().getServiceEntity() != null) {
                        sname = p.getSubscription().getServiceEntity().getName();
                    }
                    
                    String monthDisp = "DK Mới"; // Mac dinh
                    if (p.getSubscription().getCreatedAt() != null) {
                        monthDisp = p.getSubscription().getCreatedAt().toLocalDate().toString(); 
                    }
                    
                    String statusStr = "RED"; // Mac dinh la CHUA_THANH_TOAN
                    if ("DA_THANH_TOAN".equalsIgnoreCase(p.getStatus())) {
                        statusStr = "GREEN";
                    } else if ("DANG_XU_LY".equalsIgnoreCase(p.getStatus())) {
                        statusStr = "YELLOW";
                    }

                    rows.add(new AdminInvoiceView(
                            "DICH_VU", sname,
                            u.getLogin_name(),
                            u.getRoom(),
                            (u.getChungCu().getMaChungCu() != null ? u.getChungCu().getMaChungCu() : String.valueOf(u.getChungCu().getId())),
                            u.getChungCu().getName(),
                            monthDisp,
                            statusStr,
                            (p.getAmount() != null) ? p.getAmount() : BigDecimal.ZERO
                    ));
                }
            }
        }
        
        // Sap xep giảm dần theo Tháng/Ngày tạo
        rows.sort(Comparator.comparing((AdminInvoiceView x) -> x.monthDisplay).reversed());
        return rows;
    }

    public void payInvoiceMonth(Account user, String monthKey) {
        if (user == null || monthKey == null || monthKey.trim().isEmpty()) {
            return;
        }

        ApartmentMonthlyBill bill = billRepository.findByAccountIdAndMonthKey(user.getId(), monthKey)
                .orElseGet(() -> {
                    ApartmentMonthlyBill b = new ApartmentMonthlyBill();
                    b.setAccount(user);
                    b.setMonthKey(monthKey);
                    b.setStatus("CHUA_THANH_TOAN");
                    b.setDueDate(parseMonth(monthKey).atDay(10));
                    return b;
                });

        // Dong cac phi dich vu dang no trong thang do
        List<Payment> payments = paymentRepository.findBySubscriptionUserId(user.getId());
        for (Payment p : payments) {
            if (p == null || p.getSubscription() == null || p.getSubscription().getCreatedAt() == null) {
                continue;
            }
            if (!"CHUA_THANH_TOAN".equals(p.getStatus())) {
                continue;
            }
            YearMonth ym = YearMonth.from(p.getSubscription().getCreatedAt().toLocalDate());
            if (monthKey.equals(ym.toString())) {
                paymentService.updatePaymentStatus(p.getId(), "DA_THANH_TOAN");
            }
        }

        bill.setStatus("DA_THANH_TOAN");
        bill.setPaidAt(java.time.LocalDateTime.now());
        billRepository.save(bill);
    }

    private void ensureMonthlyBills(Account user) {
        YearMonth start = detectStartMonth(user);
        YearMonth end = YearMonth.now();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            String monthKey = cursor.toString();
            Optional<ApartmentMonthlyBill> existing = billRepository.findByAccountIdAndMonthKey(user.getId(), monthKey);
            if (!existing.isPresent()) {
                ApartmentMonthlyBill b = new ApartmentMonthlyBill();
                b.setAccount(user);
                b.setMonthKey(monthKey);
                b.setStatus("CHUA_THANH_TOAN");
                b.setDueDate(cursor.atDay(10)); // Han dong den ngay 10 hang thang
                ApartmentMonthlyBill saved = billRepository.save(b);
                ensureBillDetails(saved, user, cursor);
            }
            cursor = cursor.plusMonths(1);
        }
    }

    private YearMonth detectStartMonth(Account user) {
        List<Subscription> subs = subscriptionRepository.findByUserId(user.getId());
        if (subs != null && !subs.isEmpty()) {
            Subscription min = subs.stream()
                    .filter(s -> s.getCreatedAt() != null)
                    .min(Comparator.comparing(Subscription::getCreatedAt))
                    .orElse(null);
            if (min != null) {
                return YearMonth.from(min.getCreatedAt().toLocalDate());
            }
        }
        // fallback neu chua co lich su: thang hien tai
        return YearMonth.now();
    }

    private InvoiceView toView(ApartmentMonthlyBill bill, Account user) {
        YearMonth ym = parseMonth(bill.getMonthKey());
        List<ApartmentBillDetail> details = billDetailRepository.findByBillIdOrderByIdAsc(bill.getId());
        List<DetailView> detailViews = new ArrayList<>();
        BigDecimal apartmentFee = BigDecimal.ZERO;
        BigDecimal totalDetailAmount = BigDecimal.ZERO;
        for (ApartmentBillDetail d : details) {
            BigDecimal amt = (d.getAmount() == null) ? BigDecimal.ZERO : d.getAmount();
            totalDetailAmount = totalDetailAmount.add(amt);
            if ("APARTMENT_FEE".equals(d.getLineType())) {
                apartmentFee = apartmentFee.add(amt);
            }
            detailViews.add(new DetailView(
                    d.getLineType(),
                    d.getTitle(),
                    d.getQuantity(),
                    d.getUnitPrice(),
                    amt,
                    d.getNote()
            ));
        }
        ServiceFeeInfo serviceFee = calculateServiceFee(user, ym);

        BigDecimal baseDebt = apartmentFee.add(serviceFee.unpaidServiceAmount);

        String status;
        long days = 0L;
        LocalDate due = bill.getDueDate();
        if (due == null) {
            due = ym.atDay(10);
        }

        boolean serviceUnpaid = serviceFee.hasUnpaidService;
        boolean billUnpaid = !"DA_THANH_TOAN".equals(bill.getStatus());

        if (!billUnpaid && !serviceUnpaid) {
            status = "GREEN";
        } else if (LocalDate.now().isAfter(due)) {
            status = "RED";
            days = ChronoUnit.DAYS.between(due, LocalDate.now());
        } else {
            status = "YELLOW";
            days = ChronoUnit.DAYS.between(LocalDate.now(), due);
        }

        BigDecimal penalty = BigDecimal.ZERO;
        if (!"GREEN".equals(status) && baseDebt.compareTo(BigDecimal.ZERO) > 0) {
            long d = Math.max(1L, ChronoUnit.DAYS.between(ym.atDay(1), LocalDate.now()));
            long dMin = Math.min(d, 10L);
            long dMax = Math.max(0L, d - 10L);
            BigDecimal p1 = baseDebt.multiply(RATE_YELLOW).multiply(BigDecimal.valueOf(dMin));
            BigDecimal p2 = baseDebt.multiply(RATE_RED).multiply(BigDecimal.valueOf(dMax));
            penalty = p1.add(p2).setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal total = ("GREEN".equals(status)) ? BigDecimal.ZERO : baseDebt.add(penalty);

        return new InvoiceView(
                bill.getMonthKey(),
                String.format("%d/%d", ym.getMonthValue(), ym.getYear()),
                due,
                status,
                days,
                totalDetailAmount,
                penalty,
                total,
                detailViews
        );
    }

    private void ensureBillDetails(ApartmentMonthlyBill bill, Account user, YearMonth ym) {
        if (bill == null || user == null || ym == null) {
            return;
        }
        List<ApartmentBillDetail> existing = billDetailRepository.findByBillIdOrderByIdAsc(bill.getId());
        if (existing != null && !existing.isEmpty()) {
            return;
        }

        List<ApartmentBillDetail> toSave = new ArrayList<>();
        List<ApartmentFeeType> feeTypes = feeTypeRepository.findByActiveTrueOrderBySortOrderAsc();
        BigDecimal apartmentPrice = BigDecimal.valueOf(user.getChungCu() != null ? user.getChungCu().getPrice() : 0L);
        YearMonth start = detectStartMonth(user);
        boolean isStartMonth = ym.equals(start);

        for (ApartmentFeeType ft : feeTypes) {
            if (ft == null) {
                continue;
            }
            String timing = ft.getChargeTiming();
            String code = ft.getCode();
            BigDecimal amount = BigDecimal.ZERO;

            if ("HANG_THANG".equals(timing)) {
                amount = monthlyAmountByCode(code);
            } else if (isStartMonth && ("KHI_MUA".equals(timing) || "KHI_MUA_BAN".equals(timing))) {
                amount = oneTimeAmountByCode(code, apartmentPrice);
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            ApartmentBillDetail d = new ApartmentBillDetail();
            d.setBill(bill);
            d.setLineType("APARTMENT_FEE");
            d.setFeeType(ft);
            d.setTitle(ft.getName());
            d.setQuantity(BigDecimal.ONE);
            d.setUnitPrice(amount);
            d.setAmount(amount);
            d.setNote(ft.getCalcMethod());
            toSave.add(d);
        }

        List<Payment> payments = paymentRepository.findBySubscriptionUserId(user.getId());
        for (Payment p : payments) {
            if (p == null || p.getSubscription() == null || p.getSubscription().getCreatedAt() == null) {
                continue;
            }
            YearMonth pym = YearMonth.from(p.getSubscription().getCreatedAt().toLocalDate());
            if (!ym.equals(pym)) {
                continue;
            }
            BigDecimal amount = (p.getAmount() == null) ? BigDecimal.ZERO : p.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ApartmentBillDetail d = new ApartmentBillDetail();
            d.setBill(bill);
            d.setLineType("SERVICE");
            if (p.getSubscription().getServiceEntity() != null) {
                d.setService(p.getSubscription().getServiceEntity());
                d.setTitle(p.getSubscription().getServiceEntity().getName());
            } else {
                d.setTitle("Phí dịch vụ");
            }
            d.setQuantity(BigDecimal.ONE);
            d.setUnitPrice(amount);
            d.setAmount(amount);
            d.setNote(String.format("Trang thai: %s", p.getStatus()));
            toSave.add(d);
        }

        if (!toSave.isEmpty()) {
            billDetailRepository.saveAll(toSave);
        }
    }

    private ServiceFeeInfo calculateServiceFee(Account user, YearMonth ym) {
        List<Payment> payments = paymentRepository.findBySubscriptionUserId(user.getId());
        BigDecimal unpaidService = BigDecimal.ZERO;
        boolean hasUnpaid = false;

        for (Payment p : payments) {
            if (p == null || p.getSubscription() == null || p.getSubscription().getCreatedAt() == null) {
                continue;
            }
            YearMonth pym = YearMonth.from(p.getSubscription().getCreatedAt().toLocalDate());
            if (!ym.equals(pym)) {
                continue;
            }
            BigDecimal amount = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            if (!"DA_THANH_TOAN".equals(p.getStatus())) {
                unpaidService = unpaidService.add(amount);
                hasUnpaid = true;
            }
        }
        return new ServiceFeeInfo(unpaidService, hasUnpaid);
    }

    private BigDecimal monthlyAmountByCode(String code) {
        if ("PHI_QL_DICH_VU".equals(code)) {
            return new BigDecimal("300000");
        }
        if ("DIEN_NUOC_INTERNET".equals(code)) {
            return new BigDecimal("500000");
        }
        if ("PHI_TIEN_ICH".equals(code)) {
            return new BigDecimal("200000");
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal oneTimeAmountByCode(String code, BigDecimal apartmentPrice) {
        if ("LE_TRUOC_BA".equals(code)) {
            return apartmentPrice.multiply(new BigDecimal("0.005")).setScale(0, RoundingMode.HALF_UP);
        }
        if ("LE_PHI_CAP_SO_HONG".equals(code)) {
            return new BigDecimal("500000");
        }
        if ("PHI_CONG_CHUNG".equals(code)) {
            return new BigDecimal("3000000");
        }
        if ("THUE_TNCN".equals(code)) {
            return apartmentPrice.multiply(new BigDecimal("0.02")).setScale(0, RoundingMode.HALF_UP);
        }
        if ("PHI_BAO_TRI_2PCT".equals(code)) {
            return apartmentPrice.multiply(new BigDecimal("0.02")).setScale(0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private YearMonth parseMonth(String monthKey) {
        return YearMonth.parse(monthKey);
    }

    private boolean isRole(Account account, String roleName) {
        if (account == null || account.getRoles() == null) {
            return false;
        }
        for (Role r : account.getRoles()) {
            if (r != null && roleName.equals(r.getName())) {
                return true;
            }
        }
        return false;
    }

    private static class ServiceFeeInfo {
        final BigDecimal unpaidServiceAmount;
        final boolean hasUnpaidService;

        ServiceFeeInfo(BigDecimal unpaidServiceAmount, boolean hasUnpaidService) {
            this.unpaidServiceAmount = unpaidServiceAmount;
            this.hasUnpaidService = hasUnpaidService;
        }
    }

    public static class InvoiceView {
        public final String monthKey;
        public final String monthDisplay;
        public final LocalDate dueFinalDate;
        public final String status; // GREEN/YELLOW/RED
        public final long daysLeftOrOverdue;
        public final BigDecimal monthAmount;
        public final BigDecimal penalty;
        public final BigDecimal totalToPay;
        public final List<DetailView> details;

        public InvoiceView(String monthKey, String monthDisplay, LocalDate dueFinalDate, String status,
                           long daysLeftOrOverdue, BigDecimal monthAmount, BigDecimal penalty, BigDecimal totalToPay,
                           List<DetailView> details) {
            this.monthKey = monthKey;
            this.monthDisplay = monthDisplay;
            this.dueFinalDate = dueFinalDate;
            this.status = status;
            this.daysLeftOrOverdue = daysLeftOrOverdue;
            this.monthAmount = monthAmount;
            this.penalty = penalty;
            this.totalToPay = totalToPay;
            this.details = details;
        }
    }

    public static class DetailView {
        public final String lineType;
        public final String title;
        public final BigDecimal quantity;
        public final BigDecimal unitPrice;
        public final BigDecimal amount;
        public final String note;

        public DetailView(String lineType, String title, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, String note) {
            this.lineType = lineType;
            this.title = title;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.note = note;
        }
    }

    public static class AdminInvoiceView {
        public final String invoiceType; // CHUNG_CU or DICH_VU
        public final String serviceName; // Null/Tong hop if CHUNG_CU
        public final String username;
        public final String room;
        public final String apartmentCode;
        public final String apartmentName;
        public final String monthDisplay;
        public final String status;
        public final BigDecimal totalToPay;

        public AdminInvoiceView(String invoiceType, String serviceName, 
                                String username, String room, String apartmentCode, String apartmentName,
                                String monthDisplay, String status, BigDecimal totalToPay) {
            this.invoiceType = invoiceType;
            this.serviceName = serviceName;
            this.username = username;
            this.room = room;
            this.apartmentCode = apartmentCode;
            this.apartmentName = apartmentName;
            this.monthDisplay = monthDisplay;
            this.status = status;
            this.totalToPay = totalToPay;
        }
    }

    // ===== Phí cố định (dùng cho ApartmentDetailView) =====
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.02");          // 2%
    private static final BigDecimal MANAGEMENT_FEE = new BigDecimal("500000");      // 500k
    private static final BigDecimal ELECTRIC_FEE = new BigDecimal("800000");        // 800k
    private static final BigDecimal UTILITY_FEE = new BigDecimal("300000");         // 300k

    // ===== APARTMENT DETAIL VIEW (cho trang xem chi tiết căn hộ) =====
    public ApartmentDetailView getApartmentDetail(Account user) {
        if (user == null || user.getChungCu() == null) {
            return null;
        }
        ChungCu chungCu = user.getChungCu();
        BigDecimal rentPrice = BigDecimal.valueOf(chungCu.getPrice());
        BigDecimal deposit = rentPrice.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.HALF_UP);

        // Dịch vụ đã đăng ký
        List<Subscription> subs = subscriptionRepository.findByUserId(user.getId());
        List<Payment> allPayments = paymentRepository.findBySubscriptionUserId(user.getId());

        List<ServiceLineItem> serviceLines = new ArrayList<>();
        BigDecimal totalServiceFee = BigDecimal.ZERO;

        for (Subscription sub : subs) {
            if (sub.getServiceEntity() != null) {
                String name = sub.getServiceEntity().getName();
                BigDecimal price = sub.getServiceEntity().getPrice() != null ? sub.getServiceEntity().getPrice() : BigDecimal.ZERO;

                // Tìm payment status
                String status = "CHUA_THANH_TOAN";
                for (Payment p : allPayments) {
                    if (p.getSubscription() != null && p.getSubscription().getId().equals(sub.getId())) {
                        status = p.getStatus();
                        break;
                    }
                }

                serviceLines.add(new ServiceLineItem(name, price, status));
                totalServiceFee = totalServiceFee.add(price);
            }
        }

        // Tổng tất cả phí
        BigDecimal totalApartmentFees = rentPrice.add(deposit).add(MANAGEMENT_FEE).add(ELECTRIC_FEE).add(UTILITY_FEE);
        BigDecimal totalMonthly = totalApartmentFees.add(totalServiceFee);

        return new ApartmentDetailView(
                user.getLogin_name(),
                user.getRoom(),
                chungCu.getName(),
                chungCu.getMaChungCu() != null ? chungCu.getMaChungCu() : String.valueOf(chungCu.getId()),
                rentPrice,
                deposit,
                MANAGEMENT_FEE,
                ELECTRIC_FEE,
                UTILITY_FEE,
                totalServiceFee,
                serviceLines,
                totalMonthly
        );
    }

    // ===== SINGLE INVOICE VIEW (cho compatibility) =====
    public InvoiceView getInvoiceForUser(Account user) {
        List<InvoiceView> invoices = getInvoicesForUser(user);
        if (invoices == null || invoices.isEmpty()) {
            return null;
        }
        return invoices.get(0); // Trả về hóa đơn gần nhất
    }

    // ===== ADMIN: Theo dõi thanh toán dịch vụ =====
    public List<ServicePaymentAdminView> getServicePaymentsForAdmin() {
        List<Subscription> all = subscriptionRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();
        List<ServicePaymentAdminView> result = new ArrayList<>();

        for (Subscription sub : all) {
            if (sub.getUser() == null || sub.getServiceEntity() == null) continue;
            Account u = sub.getUser();
            Dichvu dv = sub.getServiceEntity();

            String status = "CHUA_THANH_TOAN";
            for (Payment p : allPayments) {
                if (p.getSubscription() != null && p.getSubscription().getId().equals(sub.getId())) {
                    status = p.getStatus();
                    break;
                }
            }

            result.add(new ServicePaymentAdminView(
                    u.getLogin_name(),
                    u.getRoom(),
                    u.getChungCu() != null ? u.getChungCu().getName() : "N/A",
                    dv.getName(),
                    dv.getPrice() != null ? dv.getPrice() : BigDecimal.ZERO,
                    status,
                    sub.getCreatedAt() != null ? sub.getCreatedAt().toString() : ""
            ));
        }
        return result;
    }

    // ===== VIEW CLASSES từ source =====

    public static class ApartmentDetailView {
        public final String username;
        public final String room;
        public final String apartmentName;
        public final String apartmentCode;
        public final BigDecimal rentPrice;
        public final BigDecimal deposit;
        public final BigDecimal managementFee;
        public final BigDecimal electricFee;
        public final BigDecimal utilityFee;
        public final BigDecimal totalServiceFee;
        public final List<ServiceLineItem> serviceLines;
        public final BigDecimal totalMonthly;

        public ApartmentDetailView(String username, String room, String apartmentName, String apartmentCode,
                                   BigDecimal rentPrice, BigDecimal deposit, BigDecimal managementFee,
                                   BigDecimal electricFee, BigDecimal utilityFee,
                                   BigDecimal totalServiceFee, List<ServiceLineItem> serviceLines,
                                   BigDecimal totalMonthly) {
            this.username = username;
            this.room = room;
            this.apartmentName = apartmentName;
            this.apartmentCode = apartmentCode;
            this.rentPrice = rentPrice;
            this.deposit = deposit;
            this.managementFee = managementFee;
            this.electricFee = electricFee;
            this.utilityFee = utilityFee;
            this.totalServiceFee = totalServiceFee;
            this.serviceLines = serviceLines;
            this.totalMonthly = totalMonthly;
        }
    }

    public static class ServiceLineItem {
        public final String serviceName;
        public final BigDecimal price;
        public final String status; // DA_THANH_TOAN or CHUA_THANH_TOAN

        public ServiceLineItem(String serviceName, BigDecimal price, String status) {
            this.serviceName = serviceName;
            this.price = price;
            this.status = status;
        }
    }

    public static class ServicePaymentAdminView {
        public final String username;
        public final String room;
        public final String apartmentName;
        public final String serviceName;
        public final BigDecimal amount;
        public final String status;
        public final String createdAt;

        public ServicePaymentAdminView(String username, String room, String apartmentName,
                                       String serviceName, BigDecimal amount, String status, String createdAt) {
            this.username = username;
            this.room = room;
            this.apartmentName = apartmentName;
            this.serviceName = serviceName;
            this.amount = amount;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
}

