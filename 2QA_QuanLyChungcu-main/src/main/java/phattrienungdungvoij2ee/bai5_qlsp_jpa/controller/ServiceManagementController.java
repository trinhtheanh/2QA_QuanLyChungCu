package phattrienungdungvoij2ee.bai5_qlsp_jpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Dichvu;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Payment;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Subscription;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Category_Dichvu;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.CategoryDichvuService;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.DichvuServiceImpl;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.PaymentService;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.SubscriptionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/quan-ly-dich-vu")
public class ServiceManagementController {

    @Autowired
    private DichvuServiceImpl dichvuService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CategoryDichvuService categoryDichvuService;

    // ===== DANH SACH DICH VU (ADMIN) =====
    @GetMapping
    public String listServices(Model model) {
        model.addAttribute("services", dichvuService.getAllServices());
        model.addAttribute("categories", categoryDichvuService.getAllCategories());
        return "quan-ly-dich-vu/list";
    }

    // ===== FORM THEM DICH VU =====
    @GetMapping("/add")
    public String showAddForm(Model model) {
        Dichvu service = new Dichvu();
        service.setCategory(new Category_Dichvu());
        model.addAttribute("service", service);
        model.addAttribute("categories", categoryDichvuService.getAllCategories());
        return "quan-ly-dich-vu/add";
    }

    // ===== LUU DICH VU (THEM/SUA) =====
    @PostMapping("/save")
    public String saveService(@ModelAttribute("service") Dichvu dichvu,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "displayPrice", required = false) String displayPrice,
                              RedirectAttributes redirectAttributes) {
        
        // Fix Spring Form không map được price do the hidden ko được cập nhật
        if (dichvu.getPrice() == null && displayPrice != null && !displayPrice.trim().isEmpty()) {
            try {
                String cleanPrice = displayPrice.replaceAll("\\D", "");
                if(!cleanPrice.isEmpty()) {
                    dichvu.setPrice(new java.math.BigDecimal(cleanPrice));
                }
            } catch (Exception ignored) {}
        }
        
        try {
            // Neu dang sua va khong upload anh moi, giu nguyen anh cu
            if (dichvu.getId() != null) {
                Dichvu existing = dichvuService.getServiceById(dichvu.getId());
                if (existing != null && (imageFile == null || imageFile.isEmpty())) {
                    dichvu.setImageUrl(existing.getImageUrl());
                }
            }
            dichvuService.saveServiceWithImage(dichvu, imageFile);
            redirectAttributes.addFlashAttribute("successMsg", "Luu dich vu thanh cong!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Luu that bai: " + e.getMessage());
        }
        return "redirect:/quan-ly-dich-vu";
    }

    // ===== FORM SUA DICH VU =====
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
                               RedirectAttributes redirectAttributes) {
        Dichvu service = dichvuService.getServiceById(id);
        if (service == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Khong tim thay dich vu!");
            return "redirect:/quan-ly-dich-vu";
        }
        if (service.getCategory() == null) {
            service.setCategory(new Category_Dichvu());
        }
        model.addAttribute("service", service);
        model.addAttribute("categories", categoryDichvuService.getAllCategories());
        return "quan-ly-dich-vu/edit";
    }

    // ===== XOA DICH VU =====
    @GetMapping("/delete/{id}")
    public String deleteService(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            dichvuService.deleteService(id);
            redirectAttributes.addFlashAttribute("successMsg", "Xoa thanh cong!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Xoa that bai: " + e.getMessage());
        }
        return "redirect:/quan-ly-dich-vu";
    }

    // ===== XEM DANH SACH NGUOI DANG KY DICH VU =====
    @GetMapping("/subscribers/{id}")
    public String viewSubscribers(@PathVariable Long id, Model model,
                                  RedirectAttributes redirectAttributes) {
        Dichvu service = dichvuService.getServiceById(id);
        if (service == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Khong tim thay dich vu!");
            return "redirect:/quan-ly-dich-vu";
        }

        List<Subscription> subscriptions = subscriptionService.getSubscriptionsByServiceId(id);

        // Lay payment cho moi subscription
        Map<Long, Payment> paymentMap = new HashMap<>();
        for (Subscription sub : subscriptions) {
            List<Payment> payments = paymentService.getPaymentsBySubscriptionId(sub.getId());
            if (!payments.isEmpty()) {
                paymentMap.put(sub.getId(), payments.get(0));
            }
        }

        model.addAttribute("service", service);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("paymentMap", paymentMap);
        return "quan-ly-dich-vu/subscribers";
    }

    // ===== TOGGLE TRANG THAI THANH TOAN =====
    @PostMapping("/toggle-payment/{paymentId}")
    public String togglePayment(@PathVariable Long paymentId,
                                @RequestParam Long serviceId,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.togglePaymentStatus(paymentId);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/quan-ly-dich-vu/subscribers/" + serviceId;
    }
}
