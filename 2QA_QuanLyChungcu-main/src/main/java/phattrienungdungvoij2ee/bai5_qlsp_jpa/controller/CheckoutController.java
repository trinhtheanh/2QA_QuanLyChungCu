package phattrienungdungvoij2ee.bai5_qlsp_jpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Account;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.CheckoutRequest;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.AccountRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.CheckoutService;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private AccountRepository accountRepository;

    // ----- USER: GUI YEU CAU TRA PHONG -----
    @PostMapping("/checkout/request")
    public String userRequestCheckout(Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String username = authentication.getName();
        Optional<Account> accOpt = accountRepository.findByLoginName(username);
        
        if (accOpt.isPresent()) {
            try {
                checkoutService.createRequest(accOpt.get());
                redirectAttributes.addFlashAttribute("successMsg", "Đã gửi yêu cầu trả phòng thành công. Ban quản lý sẽ sớm xử lý và tính toán hoàn tiền cọc cho bạn.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Không gửi được yêu cầu: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Tài khoản không hợp lệ.");
        }
        return "redirect:/Apartments";
    }

    // ----- ADMIN: XEM DANH SACH HOAN TIEN -----
    @GetMapping("/admin/checkout")
    public String adminListCheckouts(Model model) {
        model.addAttribute("requests", checkoutService.getAllRequests());
        return "checkout/list";
    }

    // ----- ADMIN: XAC NHAN VA TINH TOAN -----
    @PostMapping("/admin/checkout/approve/{id}")
    public String adminApproveCheckout(@PathVariable("id") Long id,
                                       @RequestParam(value = "penaltyFee", defaultValue = "0") BigDecimal penaltyFee,
                                       @RequestParam(value = "repairFee", defaultValue = "0") BigDecimal repairFee,
                                       @RequestParam(value = "cleaningFee", defaultValue = "0") BigDecimal cleaningFee,
                                       @RequestParam(value = "adminNote", required = false) String adminNote,
                                       RedirectAttributes redirectAttributes) {
        try {
            checkoutService.approveRequest(id, penaltyFee, repairFee, cleaningFee, adminNote);
            redirectAttributes.addFlashAttribute("successMsg", "Đã duyệt trả phòng và gỡ Cư dân ra khỏi căn hộ thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi duyệt: " + e.getMessage());
        }
        return "redirect:/admin/checkout";
    }

    // ----- ADMIN: TU CHOI -----
    @PostMapping("/admin/checkout/reject/{id}")
    public String adminRejectCheckout(@PathVariable("id") Long id,
                                      @RequestParam(value = "adminNote", required = false) String adminNote,
                                      RedirectAttributes redirectAttributes) {
        try {
            checkoutService.rejectRequest(id, adminNote);
            redirectAttributes.addFlashAttribute("successMsg", "Đã từ chối đơn trả phòng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/checkout";
    }

}
