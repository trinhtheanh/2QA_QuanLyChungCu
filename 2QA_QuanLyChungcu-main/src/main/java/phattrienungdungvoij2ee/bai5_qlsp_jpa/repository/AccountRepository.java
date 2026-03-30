package phattrienungdungvoij2ee.bai5_qlsp_jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    @Query("SELECT a FROM Account a WHERE a.login_name = :username")
    Optional<Account> findByLoginName(@Param("username") String username);

    @Query("SELECT a FROM Account a WHERE a.chungCu.id = :chungCuId")
    List<Account> findByChungCuId(@Param("chungCuId") Long chungCuId);
}