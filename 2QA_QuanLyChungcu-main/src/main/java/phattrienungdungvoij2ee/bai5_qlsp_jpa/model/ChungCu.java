package phattrienungdungvoij2ee.bai5_qlsp_jpa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Entity//
public class ChungCu {
    @Id
    @NotNull(message = "Mã chung cư (ID) không được để trống")
    private Long id;

    @NotBlank(message = "Tên chung cư không được để trống")
    @Column(nullable = false, length = 255)
    private String name;

    @NotNull(message = "Giá dịch vụ / thuê không được để trống")
    @Min(value = 1, message = "Giá không được nhỏ hơn 1")
    @Column(nullable = false)
    private long price;

    @Length(min = 0, max = 200, message = "Đường dẫn hình ảnh không quá 200 kí tự")
    @Column(length = 200)
    private String image;

    // Ma chung cu (unique) de cu dan nhap khi dang ky
    @Column(unique = true, length = 50)
    private String maChungCu;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}