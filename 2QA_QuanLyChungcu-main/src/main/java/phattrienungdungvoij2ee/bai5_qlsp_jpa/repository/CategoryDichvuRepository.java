package phattrienungdungvoij2ee.bai5_qlsp_jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Category_Dichvu;

import java.util.Optional;

@Repository
public interface CategoryDichvuRepository extends JpaRepository<Category_Dichvu, Long> {
    Optional<Category_Dichvu> findByName(String name);
}
