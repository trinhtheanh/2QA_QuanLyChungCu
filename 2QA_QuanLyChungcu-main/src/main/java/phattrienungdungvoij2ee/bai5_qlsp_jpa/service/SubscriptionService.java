package phattrienungdungvoij2ee.bai5_qlsp_jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Account;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Dichvu;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Payment;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Subscription;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.PaymentRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Dang ky dich vu cho user
    public Subscription subscribe(Account user, Dichvu dichvu) {
        // Kiem tra da dang ky chua
        if (subscriptionRepository.existsByUserIdAndServiceEntityId(user.getId(), dichvu.getId())) {
            throw new RuntimeException("Bạn đã đăng ký dịch vụ này rồi!");
        }

        // Tao subscription
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setServiceEntity(dichvu);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription = subscriptionRepository.save(subscription);

        // Tu dong tao payment record voi trang thai CHUA_THANH_TOAN
        Payment payment = new Payment();
        payment.setSubscription(subscription);
        payment.setAmount(dichvu.getPrice());
        payment.setStatus("CHUA_THANH_TOAN");
        payment.setDueDate(LocalDate.now().plusDays(30)); // Han thanh toan 30 ngay
        paymentRepository.save(payment);

        return subscription;
    }

    // Lay danh sach dang ky cua user
    public List<Subscription> getSubscriptionsByUserId(int userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    // Lay danh sach user da dang ky 1 dich vu
    public List<Subscription> getSubscriptionsByServiceId(Long serviceId) {
        return subscriptionRepository.findByServiceEntityId(serviceId);
    }

    // Kiem tra user da dang ky dich vu chua
    public boolean isSubscribed(int userId, Long serviceId) {
        return subscriptionRepository.existsByUserIdAndServiceEntityId(userId, serviceId);
    }

    // Huy dang ky
    public void unsubscribe(Long subscriptionId) {
        // Xoa payment truoc
        List<Payment> payments = paymentRepository.findBySubscriptionId(subscriptionId);
        paymentRepository.deleteAll(payments);
        // Xoa subscription
        subscriptionRepository.deleteById(subscriptionId);
    }
}
