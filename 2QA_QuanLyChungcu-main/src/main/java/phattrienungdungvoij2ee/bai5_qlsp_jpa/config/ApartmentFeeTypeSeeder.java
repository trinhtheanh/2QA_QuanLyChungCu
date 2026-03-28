package phattrienungdungvoij2ee.bai5_qlsp_jpa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.ApartmentFeeType;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.ApartmentFeeTypeRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Order(1)
public class ApartmentFeeTypeSeeder implements CommandLineRunner {

    @Autowired
    private ApartmentFeeTypeRepository repo;

    @Override
    public void run(String... args) {
        // Theo yeu cau: KHONG seed phi gui xe
        List<Seed> seeds = Arrays.asList(
                new Seed(10, "LE_TRUOC_BA", "Lệ phí trước bạ", "KHI_MUA", "Đăng ký quyền sở hữu", "Khi mua"),
                new Seed(20, "LE_PHI_CAP_SO_HONG", "Lệ phí cấp sổ hồng", "KHI_MUA", "Cấp giấy chứng nhận", "Khi mua"),
                new Seed(30, "PHI_CONG_CHUNG", "Phí công chứng", "KHI_MUA", "Hợp đồng mua bán", "Khi mua"),
                new Seed(40, "THUE_TNCN", "Thuế TNCN", "KHI_MUA_BAN", "Thuế chuyển nhượng", "Khi mua/bán"),
                new Seed(50, "PHI_BAO_TRI_2PCT", "Phí bảo trì (2%)", "KHI_MUA", "Sửa chữa, nâng cấp hạng mục chung", "2% giá trị căn hộ"),
                new Seed(60, "PHI_QL_DICH_VU", "Phí quản lý & dịch vụ", "HANG_THANG", "Vệ sinh, bảo dưỡng, an ninh", "Hàng tháng"),
                new Seed(80, "DIEN_NUOC_INTERNET", "Điện, nước, internet", "HANG_THANG", "Theo mức sử dụng", "Hàng tháng"),
                new Seed(90, "PHI_TIEN_ICH", "Phí tiện ích", "HANG_THANG", "Hồ bơi, gym, khu sinh hoạt", "Hàng tháng")
        );

        for (Seed s : seeds) {
            upsert(s);
        }
    }

    private void upsert(Seed s) {
        Optional<ApartmentFeeType> opt = repo.findByCode(s.code);
        ApartmentFeeType e = opt.orElseGet(ApartmentFeeType::new);
        e.setCode(s.code);
        e.setName(s.name);
        e.setChargeTiming(s.chargeTiming);
        e.setMainContent(s.mainContent);
        e.setCalcMethod(s.calcMethod);
        e.setSortOrder(s.sortOrder);
        e.setActive(true);
        repo.save(e);
    }

    private static class Seed {
        final int sortOrder;
        final String code;
        final String name;
        final String chargeTiming;
        final String mainContent;
        final String calcMethod;

        Seed(int sortOrder, String code, String name, String chargeTiming, String mainContent, String calcMethod) {
            this.sortOrder = sortOrder;
            this.code = code;
            this.name = name;
            this.chargeTiming = chargeTiming;
            this.mainContent = mainContent;
            this.calcMethod = calcMethod;
        }
    }
}

