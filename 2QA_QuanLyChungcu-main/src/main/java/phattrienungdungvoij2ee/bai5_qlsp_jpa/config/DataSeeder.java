package phattrienungdungvoij2ee.bai5_qlsp_jpa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.*;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Seed dữ liệu ảo để test các chức năng cơ bản:
 *  - Đăng nhập (admin.css / cư dân)
 *  - Quản lý chung cư & danh mục
 *  - Dịch vụ & đăng ký dịch vụ
 *  - Hóa đơn hàng tháng (ApartmentMonthlyBill + detail)
 *  - Thanh toán dịch vụ (Payment)
 *  - Thông báo
 *
 * Chạy SAU ApartmentFeeTypeSeeder (Order 2).
 * Idempotent: kiểm tra tồn tại trước khi insert.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final AccountRepository accountRepo;
    private final CategoryRepository categoryRepo;
    private final ChungCuRepository chungCuRepo;
    private final CategoryDichvuRepository catDichvuRepo;
    private final DichvuRepository dichvuRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PaymentRepository paymentRepo;
    private final ThongBaoRepository thongBaoRepo;
    private final ApartmentMonthlyBillRepository billRepo;
    private final ApartmentBillDetailRepository billDetailRepo;
    private final ApartmentFeeTypeRepository feeTypeRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Thứ tự quan trọng vì có FK
        List<Role> roles = seedRoles();
        List<Category> categories = seedCategories();
        List<ChungCu> chungCus = seedChungCu(categories);
        List<Account> accounts = seedAccounts(roles, chungCus);
        List<Category_Dichvu> catDvs = seedCategoryDichvu();
        List<Dichvu> dichvus = seedDichvu(catDvs);
        List<Subscription> subs = seedSubscriptions(accounts, dichvus);
        seedPayments(subs);
        seedThongBao();
        seedMonthlyBills(accounts);
    }

    // ------------------------------------------------------------------ ROLES
    private List<Role> seedRoles() {
        Role admin = roleRepo.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role r = new Role(); r.setName("ROLE_ADMIN"); return roleRepo.save(r);
        });
        Role user = roleRepo.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role(); r.setName("ROLE_USER"); return roleRepo.save(r);
        });
        return List.of(admin, user);
    }

    // --------------------------------------------------------------- CATEGORY (chung cư)
    private List<Category> seedCategories() {
        return List.of(
            findOrCreateCategory("Căn hộ cao cấp"),
            findOrCreateCategory("Căn hộ trung cấp"),
            findOrCreateCategory("Căn hộ bình dân")
        );
    }

    private Category findOrCreateCategory(String name) {
        return categoryRepo.findByName(name).orElseGet(() -> {
            Category c = new Category(); c.setName(name); return categoryRepo.save(c);
        });
    }

    // --------------------------------------------------------------- CHUNG CU
    private List<ChungCu> seedChungCu(List<Category> cats) {
        return List.of(
            findOrCreateChungCu(1L, "Vinhomes Central Park", "VH-CP", 8_500_000L, cats.get(0)),
            findOrCreateChungCu(2L, "Masteri Thảo Điền",     "MS-TD", 7_200_000L, cats.get(0)),
            findOrCreateChungCu(3L, "Saigon Pearl",           "SG-PL", 6_000_000L, cats.get(1)),
            findOrCreateChungCu(4L, "Him Lam Riverside",      "HL-RV", 4_500_000L, cats.get(2))
        );
    }

    private ChungCu findOrCreateChungCu(Long id, String name, String ma, long price, Category cat) {
        return chungCuRepo.findByMaChungCu(ma).orElseGet(() -> {
            ChungCu cc = new ChungCu();
            cc.setId(id);
            cc.setName(name);
            cc.setMaChungCu(ma);
            cc.setPrice(price);
            cc.setCategory(cat);
            return chungCuRepo.save(cc);
        });
    }

    // --------------------------------------------------------------- ACCOUNTS
    private List<Account> seedAccounts(List<Role> roles, List<ChungCu> ccs) {
        Role adminRole = roles.get(0);
        Role userRole  = roles.get(1);

        Account admin = findOrCreateAccount("admin", "Admin@123", Set.of(adminRole), null, null);
        Account cu1   = findOrCreateAccount("nguyen.van.a", "User@123", Set.of(userRole), ccs.get(0), "A1-101");
        Account cu2   = findOrCreateAccount("tran.thi.b",   "User@123", Set.of(userRole), ccs.get(0), "A1-202");
        Account cu3   = findOrCreateAccount("le.van.c",     "User@123", Set.of(userRole), ccs.get(1), "B2-305");
        Account cu4   = findOrCreateAccount("pham.thi.d",   "User@123", Set.of(userRole), ccs.get(2), "C3-410");

        return List.of(admin, cu1, cu2, cu3, cu4);
    }

    private Account findOrCreateAccount(String loginName, String rawPwd,
                                        Set<Role> roles, ChungCu cc, String room) {
        return accountRepo.findByLoginName(loginName).orElseGet(() -> {
            Account a = new Account();
            a.setLogin_name(loginName);
            a.setPassword(passwordEncoder.encode(rawPwd));
            a.setRoles(roles);
            a.setChungCu(cc);
            a.setRoom(room);
            return accountRepo.save(a);
        });
    }

    // --------------------------------------------------------------- CATEGORY DICH VU
    private List<Category_Dichvu> seedCategoryDichvu() {
        return List.of(
            findOrCreateCatDv("Vệ sinh & Môi trường", "Dịch vụ dọn dẹp, thu gom rác"),
            findOrCreateCatDv("An ninh & Bảo vệ",     "Dịch vụ bảo vệ 24/7"),
            findOrCreateCatDv("Tiện ích nội khu",      "Hồ bơi, gym, sân chơi"),
            findOrCreateCatDv("Sửa chữa & Bảo trì",   "Điện, nước, điều hòa")
        );
    }

    private Category_Dichvu findOrCreateCatDv(String name, String desc) {
        return catDichvuRepo.findByName(name).orElseGet(() -> {
            Category_Dichvu c = new Category_Dichvu();
            c.setName(name); c.setDescription(desc);
            return catDichvuRepo.save(c);
        });
    }

    // --------------------------------------------------------------- DICH VU
    private List<Dichvu> seedDichvu(List<Category_Dichvu> cats) {
        return List.of(
            findOrCreateDichvu("Dọn vệ sinh căn hộ",    "Vệ sinh toàn bộ căn hộ định kỳ",  "Công ty Sạch Sẽ",  "lần",  "FIXED",  new BigDecimal("250000"), cats.get(0)),
            findOrCreateDichvu("Thu gom rác hàng ngày",  "Thu gom rác tận cửa mỗi ngày",     "Công ty Sạch Sẽ",  "tháng","FIXED",  new BigDecimal("80000"),  cats.get(0)),
            findOrCreateDichvu("Bảo vệ xe máy",          "Giữ xe máy trong hầm 24/7",        "BV An Toàn",       "tháng","FIXED",  new BigDecimal("150000"), cats.get(1)),
            findOrCreateDichvu("Thẻ hồ bơi",             "Sử dụng hồ bơi không giới hạn",   "Khu tiện ích",     "tháng","FIXED",  new BigDecimal("300000"), cats.get(2)),
            findOrCreateDichvu("Thẻ gym",                 "Sử dụng phòng gym không giới hạn","Khu tiện ích",     "tháng","FIXED",  new BigDecimal("350000"), cats.get(2)),
            findOrCreateDichvu("Sửa điện nước",          "Sửa chữa điện nước theo yêu cầu", "Đội kỹ thuật",     "lần",  "VARIABLE",new BigDecimal("200000"), cats.get(3))
        );
    }

    private Dichvu findOrCreateDichvu(String name, String desc, String provider,
                                      String unit, String costType, BigDecimal price,
                                      Category_Dichvu cat) {
        return dichvuRepo.findByName(name).orElseGet(() -> {
            Dichvu d = new Dichvu();
            d.setName(name); d.setDescription(desc); d.setProvider(provider);
            d.setUnit(unit); d.setCostType(costType); d.setCostValue(price);
            d.setPrice(price); d.setCategory(cat);
            return dichvuRepo.save(d);
        });
    }

    // --------------------------------------------------------------- SUBSCRIPTIONS
    private List<Subscription> seedSubscriptions(List<Account> accounts, List<Dichvu> dichvus) {
        // accounts: [admin.css, cu1, cu2, cu3, cu4]
        // cu1 đăng ký: dọn vệ sinh, thẻ hồ bơi, thẻ gym
        // cu2 đăng ký: thu gom rác, bảo vệ xe
        // cu3 đăng ký: thẻ hồ bơi
        Account cu1 = accounts.get(1);
        Account cu2 = accounts.get(2);
        Account cu3 = accounts.get(3);

        Subscription s1 = findOrCreateSub(cu1, dichvus.get(0)); // dọn vệ sinh
        Subscription s2 = findOrCreateSub(cu1, dichvus.get(3)); // hồ bơi
        Subscription s3 = findOrCreateSub(cu1, dichvus.get(4)); // gym
        Subscription s4 = findOrCreateSub(cu2, dichvus.get(1)); // thu gom rác
        Subscription s5 = findOrCreateSub(cu2, dichvus.get(2)); // bảo vệ xe
        Subscription s6 = findOrCreateSub(cu3, dichvus.get(3)); // hồ bơi

        return List.of(s1, s2, s3, s4, s5, s6);
    }

    private Subscription findOrCreateSub(Account user, Dichvu dv) {
        if (subscriptionRepo.existsByUserIdAndServiceEntityId(user.getId(), dv.getId())) {
            return subscriptionRepo.findByUserId(user.getId()).stream()
                    .filter(s -> s.getServiceEntity().getId().equals(dv.getId()))
                    .findFirst().orElseThrow();
        }
        Subscription s = new Subscription();
        s.setUser(user);
        s.setServiceEntity(dv);
        // Đặt createdAt = đầu tháng trước để cả tháng trước lẫn tháng này
        // đều có dòng phí dịch vụ trong hóa đơn (vì service dùng subscription.createdAt để xác định tháng)
        s.setCreatedAt(LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
        return subscriptionRepo.save(s);
    }

    // --------------------------------------------------------------- PAYMENTS
    private void seedPayments(List<Subscription> subs) {
        // subscription.createdAt = đầu tháng trước => payment tháng trước đã thanh toán
        // Tháng này: tạo thêm payment mới với dueDate cuối tháng này, chưa thanh toán
        LocalDate lastMonthEnd = LocalDate.now().minusMonths(1).withDayOfMonth(
                LocalDate.now().minusMonths(1).lengthOfMonth());
        LocalDate thisMonthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        for (Subscription sub : subs) {
            BigDecimal amount = sub.getServiceEntity().getPrice();
            List<Payment> existing = paymentRepo.findBySubscriptionId(sub.getId());

            // Tháng trước - đã thanh toán (khớp với subscription.createdAt tháng trước)
            boolean hasLastMonth = existing.stream().anyMatch(p ->
                    p.getDueDate() != null && p.getDueDate().getMonthValue() == lastMonthEnd.getMonthValue()
                    && p.getDueDate().getYear() == lastMonthEnd.getYear());
            if (!hasLastMonth) {
                Payment p1 = new Payment();
                p1.setSubscription(sub); p1.setAmount(amount);
                p1.setStatus("DA_THANH_TOAN");
                p1.setDueDate(lastMonthEnd);
                paymentRepo.save(p1);
            }

            // Tháng này - chưa thanh toán
            boolean hasThisMonth = existing.stream().anyMatch(p ->
                    p.getDueDate() != null && p.getDueDate().getMonthValue() == thisMonthEnd.getMonthValue()
                    && p.getDueDate().getYear() == thisMonthEnd.getYear());
            if (!hasThisMonth) {
                Payment p2 = new Payment();
                p2.setSubscription(sub); p2.setAmount(amount);
                p2.setStatus("CHUA_THANH_TOAN");
                p2.setDueDate(thisMonthEnd);
                paymentRepo.save(p2);
            }
        }
    }

    // --------------------------------------------------------------- THONG BAO
    private void seedThongBao() {
        if (thongBaoRepo.count() > 0) return;

        Object[][] data = {
            {"[KHẨN] Cúp điện bảo trì hệ thống", "Ngày 30/03/2026 từ 8h-12h, toàn bộ tòa A sẽ cúp điện để bảo trì định kỳ. Cư dân vui lòng chủ động.", "KHAN_CAP", true, "Ban Quản Lý", "Vinhomes Central Park"},
            {"Thông báo nộp phí tháng 3/2026", "Hạn nộp phí quản lý tháng 3/2026 là ngày 31/03/2026. Cư dân vui lòng nộp đúng hạn để tránh phát sinh phí trễ hạn.", "PHI_DICH_VU", false, "Kế Toán", null},
            {"Khai trương khu gym mới", "Khu gym tầng 5 tòa B đã được nâng cấp và khai trương lại từ ngày 01/04/2026. Cư dân đăng ký thẻ gym được miễn phí tháng đầu.", "SU_KIEN", false, "Ban Quản Lý", null},
            {"Lịch vệ sinh hồ bơi tháng 4", "Hồ bơi sẽ tạm ngưng hoạt động từ 01/04 đến 03/04/2026 để vệ sinh định kỳ. Xin lỗi vì sự bất tiện này.", "BAO_TRI", false, "Đội Kỹ Thuật", null},
            {"Quy định mới về giữ xe", "Từ 01/04/2026, tất cả xe máy phải đăng ký biển số với ban quản lý. Xe không đăng ký sẽ không được vào hầm.", "QUY_DINH", true, "Ban Quản Lý", null},
        };

        for (Object[] row : data) {
            ThongBao tb = new ThongBao();
            tb.setTieuDe((String) row[0]);
            tb.setNoiDung((String) row[1]);
            tb.setLoaiThongBao((String) row[2]);
            tb.setGhim((Boolean) row[3]);
            tb.setNguoiTao((String) row[4]);
            tb.setKhuChungCu((String) row[5]);
            tb.setNgayTao(LocalDateTime.now().minusDays((long)(Math.random() * 10)));
            thongBaoRepo.save(tb);
        }
    }

    // --------------------------------------------------------------- MONTHLY BILLS
    private void seedMonthlyBills(List<Account> accounts) {
        // Seed hóa đơn tháng trước và tháng này cho cu1, cu2, cu3
        String lastMonthKey = LocalDate.now().minusMonths(1).toString().substring(0, 7);
        String thisMonthKey = LocalDate.now().toString().substring(0, 7);

        ApartmentFeeType feeQl  = feeTypeRepo.findByCode("PHI_QL_DICH_VU").orElse(null);
        ApartmentFeeType feeDn  = feeTypeRepo.findByCode("DIEN_NUOC_INTERNET").orElse(null);
        ApartmentFeeType feeTu  = feeTypeRepo.findByCode("PHI_TIEN_ICH").orElse(null);

        for (int i = 1; i <= 3; i++) {
            Account acc = accounts.get(i);
            // Tháng trước - đã thanh toán
            createBillIfAbsent(acc, lastMonthKey, "DA_THANH_TOAN",
                    LocalDate.now().minusMonths(1).withDayOfMonth(28),
                    LocalDateTime.now().minusDays(5), feeQl, feeDn, feeTu);
            // Tháng này - chưa thanh toán
            createBillIfAbsent(acc, thisMonthKey, "CHUA_THANH_TOAN",
                    LocalDate.now().withDayOfMonth(28),
                    null, feeQl, feeDn, feeTu);
        }
    }

    private void createBillIfAbsent(Account acc, String monthKey, String status,
                                    LocalDate dueDate, LocalDateTime paidAt,
                                    ApartmentFeeType feeQl, ApartmentFeeType feeDn, ApartmentFeeType feeTu) {
        if (billRepo.findByAccountIdAndMonthKey(acc.getId(), monthKey).isPresent()) return;

        ApartmentMonthlyBill bill = new ApartmentMonthlyBill();
        bill.setAccount(acc);
        bill.setMonthKey(monthKey);
        bill.setStatus(status);
        bill.setDueDate(dueDate);
        bill.setPaidAt(paidAt);
        bill = billRepo.save(bill);

        // Chi tiết hóa đơn
        if (feeQl != null) saveBillDetail(bill, "APARTMENT_FEE", feeQl, null, "Phí quản lý & dịch vụ", BigDecimal.ONE, new BigDecimal("500000"));
        if (feeDn != null) saveBillDetail(bill, "APARTMENT_FEE", feeDn, null, "Điện, nước, internet",   BigDecimal.ONE, new BigDecimal("750000"));
        if (feeTu != null) saveBillDetail(bill, "APARTMENT_FEE", feeTu, null, "Phí tiện ích",           BigDecimal.ONE, new BigDecimal("200000"));
    }

    private void saveBillDetail(ApartmentMonthlyBill bill, String lineType,
                                ApartmentFeeType feeType, Dichvu service,
                                String title, BigDecimal qty, BigDecimal unitPrice) {
        ApartmentBillDetail d = new ApartmentBillDetail();
        d.setBill(bill);
        d.setLineType(lineType);
        d.setFeeType(feeType);
        d.setService(service);
        d.setTitle(title);
        d.setQuantity(qty);
        d.setUnitPrice(unitPrice);
        d.setAmount(unitPrice.multiply(qty));
        billDetailRepo.save(d);
    }
}
