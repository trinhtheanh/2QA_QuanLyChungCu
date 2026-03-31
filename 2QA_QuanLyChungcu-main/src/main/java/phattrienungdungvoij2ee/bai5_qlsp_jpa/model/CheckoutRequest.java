package phattrienungdungvoij2ee.bai5_qlsp_jpa.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class CheckoutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chungcu_id")
    private ChungCu chungCu;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    // PENDING, APPROVED, REJECTED
    @Column(nullable = false)
    private String status = "PENDING";

    // Tiền cọc lúc ban đầu
    @Column(precision = 19, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    // Phí phạt vi phạm hợp đồng
    @Column(precision = 19, scale = 2)
    private BigDecimal penaltyFee = BigDecimal.ZERO;

    // Phí sửa chữa hư hỏng
    @Column(precision = 19, scale = 2)
    private BigDecimal repairFee = BigDecimal.ZERO;

    // Phí vệ sinh
    @Column(precision = 19, scale = 2)
    private BigDecimal cleaningFee = BigDecimal.ZERO;

    // Tổng tiền hoàn trả cuối cùng (Refund = Deposit - Penalty - Repair - Cleaning)
    @Column(precision = 19, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    // Ghi chú của admin khi duyệt
    @Column(length = 500)
    private String adminNote;
}
