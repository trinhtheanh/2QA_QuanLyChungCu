package phattrienungdungvoij2ee.bai5_qlsp_jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Dichvu;

import java.util.Optional;

@Repository
public interface DichvuRepository extends JpaRepository<Dichvu, Long> {
    Optional<Dichvu> findByName(String name);
}
