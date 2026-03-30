package phattrienungdungvoij2ee.bai5_qlsp_jpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.AccountRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.ApartmentInvoiceService;

@Controller
public class HoaDonAdminController {

    @Autowired
    private ApartmentInvoiceService invoiceService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/hoa-don/admin")
    public String allInvoices(Model model) {
        model.addAttribute("items", invoiceService.getInvoicesForAdminManager(accountRepository.findAll()));
        return "hoa-don/admin";
    }
}
