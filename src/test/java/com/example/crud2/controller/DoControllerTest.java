package com.example.crud2.controller;

import com.example.crud2.entity.DoIt;
import com.example.crud2.service.DoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DoController.class)
class DoControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	DoService doService;

	@Test
	void addForm() throws Exception {
		mockMvc.perform(get("/mains/add"))
				.andExpect(status().isOk())
				.andExpect(view().name("mains/add"))
				.andExpect(model().attributeExists("doDto"));
	}

	@Test
	void list() throws Exception {
		when(doService.findAll()).thenReturn(List.of(new DoIt(1L, "t", "c")));

		mockMvc.perform(get("/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("mains/doList"))
				.andExpect(model().attribute("DoList", contains(
						allOf(
								hasProperty("num", is(1L)),
								hasProperty("title", is("t")),
								hasProperty("content", is("c"))))));

		verify(doService).findAll();
	}

	@Test
	void detail_when_found() throws Exception {
		when(doService.findById(5L)).thenReturn(Optional.of(new DoIt(5L, "제목", "본문")));

		mockMvc.perform(get("/list/5"))
				.andExpect(status().isOk())
				.andExpect(view().name("mains/detail"))
				.andExpect(model().attribute("detail", allOf(
						hasProperty("num", is(5L)),
						hasProperty("title", is("제목")),
						hasProperty("content", is("본문")))));
	}

	@Test
	void detail_when_missing_redirects() throws Exception {
		when(doService.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/list/99"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/list"));
	}

	@Test
	void updateForm_when_found() throws Exception {
		when(doService.findById(2L)).thenReturn(Optional.of(new DoIt(2L, "a", "b")));

		mockMvc.perform(get("/list/2/edit"))
				.andExpect(status().isOk())
				.andExpect(view().name("mains/edit"))
				.andExpect(model().attribute("editDto", allOf(
						hasProperty("num", is(2L)),
						hasProperty("title", is("a")),
						hasProperty("content", is("b")))));
	}

	@Test
	void delete_success_sets_flash() throws Exception {
		when(doService.delete(3L)).thenReturn(true);

		mockMvc.perform(get("/list/3/delete"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/list"))
				.andExpect(flash().attribute("msg", "삭제가 완료되었습니다."));
	}

	@Test
	void create_redirects_to_detail() throws Exception {
		when(doService.create(any(DoDto.class))).thenReturn(new DoIt(10L, "x", "y"));

		mockMvc.perform(post("/mains/create")
						.param("title", "x")
						.param("content", "y"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/list/10"));
	}

	@Test
	void update_redirects_when_service_returns_value() throws Exception {
		when(doService.update(any(DoDto.class))).thenReturn(Optional.of(new DoIt(7L, "u", "v")));

		mockMvc.perform(post("/mains/update")
						.param("num", "7")
						.param("title", "u")
						.param("content", "v"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/list/7"));
	}

	@Test
	void update_redirects_to_list_when_missing() throws Exception {
		when(doService.update(any(DoDto.class))).thenReturn(Optional.empty());

		mockMvc.perform(post("/mains/update")
						.param("num", "1")
						.param("title", "u")
						.param("content", "v"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/list"));
	}
}
