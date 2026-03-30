package phattrienungdungvoij2ee.bai5_qlsp_jpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Account;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.ChungCu;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.AccountRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.ChungCuRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/Apartments")
public class ChungCuController {

    @Autowired
    private ChungCuService chungCuService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private DichvuService dichvuService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ApartmentInvoiceService invoiceService;

    @Autowired
    private ChungCuRepository chungCuRepository;

    @Autowired
    private PaymentService paymentService;

    // Thu muc luu anh: src/main/resources/static/uploads/
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @GetMapping
    public String listChungCus(Model model, Authentication authentication) {
        long totalPaidBills = paymentService.countPaidPayments();
        // Thong bao cho trang chu
        model.addAttribute("thongbaos", thongBaoService.getAllThongBao());

        // Stat card counts for dashboard
        model.addAttribute("totalApartments", chungCuService.getAllChungCus().size());
        model.addAttribute("totalThongBao", thongBaoService.getAllThongBao().size());
        model.addAttribute("totalCategories", categoryService.getAllCategories().size());
        model.addAttribute("categories", categoryService.getAllCategories()); // Thêm list loại cho dropdown
        model.addAttribute("totalPaidBills", totalPaidBills);
        try {
            model.addAttribute("totalServices", dichvuService.getAllServices().size());
        } catch (Exception e) {
            model.addAttribute("totalServices", 0);
        }

        // Phan quyen hien thi chung cu
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isManager = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"));

        if (isAdmin || isManager) {
            // Admin va Manager xem tat ca chung cu
            model.addAttribute("products", chungCuService.getAllChungCus());
            model.addAttribute("isAdmin", isAdmin);
        } else {
            // User (cu dan) chi xem chung cu cua minh
            Optional<Account> accountOpt = accountRepository.findByLoginName(authentication.getName());
            if (accountOpt.isPresent() && accountOpt.get().getChungCu() != null) {
                List<ChungCu> myChungCu = new ArrayList<>();
                myChungCu.add(accountOpt.get().getChungCu());
                model.addAttribute("products", myChungCu);
            } else {
                model.addAttribute("products", new ArrayList<>());
            }
            model.addAttribute("isAdmin", false);
        }

        return "product/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new ChungCu());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "product/add";
    }

    @PostMapping("/save")
    public String saveChungCu(
            @ModelAttribute("product") ChungCu chungCu,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "displayPrice", required = false) String displayPrice,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        // Fix truong hop Spring Form khong nhan duoc gia tri cua "price" qua the hidden
        if (chungCu.getPrice() == 0 && displayPrice != null && !displayPrice.trim().isEmpty()) {
            try {
                long parsedPrice = Long.parseLong(displayPrice.replaceAll("\\D", ""));
                chungCu.setPrice(parsedPrice);
            } catch (Exception ignored) {}
        }

        // Kiem tra day la trang Add hay Edit thong qua Referer Header hoac Hidden input
        String referer = request.getHeader("Referer");
        boolean isEditAction = false;
        
        if (referer != null && referer.contains("/edit/")) {
            isEditAction = true;
        }

        ChungCu existingDb = null;
        if (chungCu.getId() != null) {
            existingDb = chungCuService.getChungCuById(chungCu.getId());
        }

        if (!isEditAction) {
            // ĐANG Ở TRANG THÊM MỚI (ADD)
            // 1. Kiem tra trung ID he thong
            if (existingDb != null) {
                redirectAttributes.addFlashAttribute("error", 
                    String.format("Mã hệ thống (ID) '%d' đã tồn tại! Vui lòng nhập ID khác.", chungCu.getId()));
                return "redirect:/Apartments/add";
            }
            
            // 2. Kiem tra trung maChungCu hien thi
            if (chungCu.getMaChungCu() != null && !chungCu.getMaChungCu().trim().isEmpty()) {
                Optional<ChungCu> existingMa = chungCuRepository.findByMaChungCu(chungCu.getMaChungCu().trim());
                if (existingMa.isPresent()) {
                    redirectAttributes.addFlashAttribute("error",
                            String.format("Mã chung cư hiển thị '%s' đã tồn tại! Vui lòng nhập mã khác.", chungCu.getMaChungCu()));
                    return "redirect:/Apartments/add";
                }
            }
        } else {
            // ĐANG Ở TRANG SỬA (EDIT)
            // Kiem tra maChungCu trung voi can ho KHAC (khac id hien tai)
            if (chungCu.getMaChungCu() != null && !chungCu.getMaChungCu().trim().isEmpty()) {
                Optional<ChungCu> existingMa = chungCuRepository.findByMaChungCu(chungCu.getMaChungCu().trim());
                if (existingMa.isPresent() && !existingMa.get().getId().equals(chungCu.getId())) {
                    redirectAttributes.addFlashAttribute("error",
                            String.format("Mã chung cư hiển thị '%s' đã được sử dụng bởi căn hộ hiện có khác!", chungCu.getMaChungCu()));
                    return "redirect:/Apartments/edit/" + chungCu.getId();
                }
            }
        }

        // Xu ly upload anh neu co file moi
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Tao thu muc neu chua co
                File uploadDir = new File(UPLOAD_DIR);
                if (!uploadDir.exists()) uploadDir.mkdirs();

                // Tao ten file unique
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                Files.write(filePath, imageFile.getBytes());

                // Luu duong dan vao DB
                chungCu.setImage("/uploads/" + fileName);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Upload ảnh thất bại: " + e.getMessage());
                return "redirect:/Apartments";
            }
        }

        chungCuService.saveChungCu(chungCu);
        return "redirect:/Apartments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("product", chungCuService.getChungCuById(id));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "product/edit";
    }

    @GetMapping("/delete/{id}")
    public String deleteChungCu(@PathVariable("id") Long id) {
        chungCuService.deleteChungCu(id);
        return "redirect:/Apartments";
    }

    // ===== XEM CHI TIET CAN HO =====
    @GetMapping("/detail/{id}")
    public String apartmentDetail(@PathVariable("id") Long id, Model model,
                                  Authentication authentication, RedirectAttributes redirectAttributes) {
        ChungCu chungCu = chungCuService.getChungCuById(id);
        if (chungCu == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy căn hộ!");
            return "redirect:/Apartments";
        }

        boolean isAdmin = authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN"));

        List<ApartmentInvoiceService.ApartmentDetailView> details = new ArrayList<>();

        if (isAdmin) {
            // Admin xem tat ca cu dan trong can ho
            List<Account> residents = accountRepository.findByChungCuId(id);
            for (Account user : residents) {
                ApartmentInvoiceService.ApartmentDetailView detail = invoiceService.getApartmentDetail(user);
                if (detail != null) {
                    details.add(detail);
                }
            }
        } else {
            // User chi xem chinh minh
            Optional<Account> accountOpt = accountRepository.findByLoginName(authentication.getName());
            if (accountOpt.isPresent() && accountOpt.get().getChungCu() != null
                    && accountOpt.get().getChungCu().getId().equals(id)) {
                ApartmentInvoiceService.ApartmentDetailView detail = invoiceService.getApartmentDetail(accountOpt.get());
                if (detail != null) {
                    details.add(detail);
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "Bạn không có quyền xem căn hộ này!");
                return "redirect:/Apartments";
            }
        }

        model.addAttribute("chungCu", chungCu);
        model.addAttribute("details", details);
        model.addAttribute("isAdmin", isAdmin);
        return "product/detail";
    }
}