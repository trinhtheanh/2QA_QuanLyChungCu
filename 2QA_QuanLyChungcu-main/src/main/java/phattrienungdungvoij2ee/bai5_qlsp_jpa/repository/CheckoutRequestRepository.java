package phattrienungdungvoij2ee.bai5_qlsp_jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.CheckoutRequest;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutRequestRepository extends JpaRepository<CheckoutRequest, Long> {
    List<CheckoutRequest> findByAccountId(int accountId);
    List<CheckoutRequest> findByAccountIdOrderByIdDesc(int accountId);
    List<CheckoutRequest> findAllByOrderByIdDesc();
    Optional<CheckoutRequest> findByAccountIdAndStatus(int accountId, String status);
}
