package phattrienungdungvoij2ee.bai5_qlsp_jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Account;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.CheckoutRequest;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.ChungCu;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.AccountRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.CheckoutRequestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    @Autowired
    private CheckoutRequestRepository checkoutRequestRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.SubscriptionRepository subscriptionRepository;

    @Autowired
    private phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.ApartmentMonthlyBillRepository billRepository;

    // Tiền cọc lúc đầu được tính bằng 1 tháng tiền thuê
    public CheckoutRequest createRequest(Account account) throws Exception {
        if (account.getChungCu() == null) {
            throw new Exception("Bạn không sở hữu căn hộ nào để trả.");
        }
        
        // Kiem tra xem da co yeu cau nao dang cho chua
        Optional<CheckoutRequest> existingPending = checkoutRequestRepository.findByAccountIdAndStatus(account.getId(), "PENDING");
        if (existingPending.isPresent()) {
            throw new Exception("Bạn đã gửi yêu cầu trả phòng rồi, vui lòng chờ ban quản lý phê duyệt.");
        }

        CheckoutRequest request = new CheckoutRequest();
        request.setAccount(account);
        request.setChungCu(account.getChungCu());
        request.setRequestDate(LocalDateTime.now());
        request.setStatus("PENDING");

        BigDecimal rentPrice = BigDecimal.valueOf(account.getChungCu().getPrice());
        request.setDepositAmount(rentPrice); // Cọc bằng 1 tháng tiền thuê

        return checkoutRequestRepository.save(request);
    }

    public List<CheckoutRequest> getAllRequests() {
        return checkoutRequestRepository.findAllByOrderByIdDesc();
    }

    public CheckoutRequest getRequestById(Long id) {
        return checkoutRequestRepository.findById(id).orElse(null);
    }

    @Transactional
    public CheckoutRequest approveRequest(Long id, BigDecimal penaltyFee, BigDecimal repairFee, BigDecimal cleaningFee, String adminNote) throws Exception {
        CheckoutRequest request = getRequestById(id);
        if (request == null) {
            throw new Exception("Không tìm thấy đơn trả phòng.");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new Exception("Đơn này đã được xử lý.");
        }

        // Tinh toan hoan tien
        BigDecimal deposit = request.getDepositAmount();
        BigDecimal totalDeduction = penaltyFee.add(repairFee).add(cleaningFee);
        BigDecimal refund = deposit.subtract(totalDeduction);
        
        // Cap nhat don
        request.setPenaltyFee(penaltyFee);
        request.setRepairFee(repairFee);
        request.setCleaningFee(cleaningFee);
        request.setRefundAmount(refund);
        request.setAdminNote(adminNote);
        request.setStatus("APPROVED");

        // Go lien ket can ho ngay lap tuc
        Account account = request.getAccount();
        if (account != null) {
            // 1. Cat dut khoa ngoai tren bang Yeu Cau (De lam lich su an danh)
            List<CheckoutRequest> reqs = checkoutRequestRepository.findByAccountId(account.getId());
            for (CheckoutRequest r : reqs) { r.setAccount(null); }
            checkoutRequestRepository.saveAll(reqs);

            // 2. Cat dut khoa ngoai tren bang Dang ky Dich vu
            List<phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Subscription> subs = subscriptionRepository.findByUserId(account.getId());
            for (phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Subscription s : subs) { s.setUser(null); }
            subscriptionRepository.saveAll(subs);

            // 3. Cat dut khoa ngoai tren bang Hoa don thang
            List<phattrienungdungvoij2ee.bai5_qlsp_jpa.model.ApartmentMonthlyBill> bills = billRepository.findByAccountIdOrderByMonthKeyDesc(account.getId());
            for (phattrienungdungvoij2ee.bai5_qlsp_jpa.model.ApartmentMonthlyBill b : bills) { b.setAccount(null); }
            billRepository.saveAll(bills);

            // 4. Xoa Roles trung gian
            account.getRoles().clear();
            accountRepository.save(account);

            // 5. Xoa han Account ra khoi he thong
            accountRepository.delete(account);
        }

        return checkoutRequestRepository.save(request);
    }

    @Transactional
    public CheckoutRequest rejectRequest(Long id, String adminNote) throws Exception {
        CheckoutRequest request = getRequestById(id);
        if (request == null) {
            throw new Exception("Không tìm thấy đơn trả phòng.");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new Exception("Đơn này đã được xử lý.");
        }

        request.setStatus("REJECTED");
        request.setAdminNote(adminNote);

        return checkoutRequestRepository.save(request);
    }
}
