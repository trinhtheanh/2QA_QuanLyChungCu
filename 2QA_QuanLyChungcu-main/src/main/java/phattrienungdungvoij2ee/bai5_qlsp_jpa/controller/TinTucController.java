package phattrienungdungvoij2ee.bai5_qlsp_jpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.ThongBaoService;

@Controller
@RequestMapping("/tintuc")
public class TinTucController {

    @Autowired
    private ThongBaoService thongBaoService;

    @GetMapping
    public String listTinTuc(@RequestParam(value = "loai", required = false) String loai, Model model) {
        if (loai != null && !loai.isEmpty()) {
            model.addAttribute("thongbaos", thongBaoService.getByLoai(loai));
        } else {
            model.addAttribute("thongbaos", thongBaoService.getAllThongBao());
        }
        model.addAttribute("currentLoai", loai);
        return "tintuc/list";
    }
}
