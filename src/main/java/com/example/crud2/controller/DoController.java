package com.example.crud2.controller;

import com.example.crud2.dto.DoDto;
import com.example.crud2.entity.DoIt;
import com.example.crud2.service.DoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DoController {

	private final DoService doService;

	public DoController(DoService doService) {
		this.doService = doService;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/list";
	}

	@GetMapping("/mains/add")
	public String addForm(Model model) {
		model.addAttribute("doDto", new DoDto());
		return "mains/add";
	}

	@GetMapping("/list/{num}")
	public String detail(@PathVariable("num") Long num, Model model) {
		return doService.findById(num)
				.map(doIt -> {
					model.addAttribute("detail", doIt);
					return "mains/detail";
				})
				.orElse("redirect:/list");
	}

	@GetMapping("/list")
	public String list(Model model) {
		List<DoIt> doList = doService.findAll();
		model.addAttribute("DoList", doList);
		return "mains/doList";
	}

	@GetMapping("/list/{num}/edit")
	public String updateForm(@PathVariable("num") Long num, Model model) {
		return doService.findById(num)
				.map(toDo -> {
					model.addAttribute("editDto", new DoDto(toDo.getNum(), toDo.getTitle(), toDo.getContent()));
					return "mains/edit";
				})
				.orElse("redirect:/list");
	}

	@GetMapping("/list/{num}/delete")
	public String delete(@PathVariable("num") Long num, RedirectAttributes rttr) {
		if (doService.delete(num)) {
			rttr.addFlashAttribute("msg", "삭제가 완료되었습니다.");
		}
		return "redirect:/list";
	}

	@PostMapping("/mains/create")
	public String create(DoDto dto) {
		DoIt saved = doService.create(dto);
		return "redirect:/list/" + saved.getNum();
	}

	@PostMapping("/mains/update")
	public String update(DoDto dto) {
		return doService.update(dto)
				.map(saved -> "redirect:/list/" + saved.getNum())
				.orElse("redirect:/list");
	}
}
