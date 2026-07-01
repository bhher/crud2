package com.example.crud2.service;

import com.example.crud2.dto.DoDto;
import com.example.crud2.entity.DoIt;
import com.example.crud2.repository.DoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DoService.class)
class DoServiceTest {

	@Autowired
	DoService doService;

	@Autowired
	DoRepository doRepository;

	@Test
	void create_and_findAll() {
		DoIt saved = doService.create(new DoDto(null, "제목", "내용"));

		assertThat(saved.getNum()).isNotNull();
		List<DoIt> all = doService.findAll();
		assertThat(all).hasSize(1);
		assertThat(all.get(0).getTitle()).isEqualTo("제목");
	}

	@Test
	void findById_returns_entity() {
		DoIt saved = doRepository.save(new DoIt(null, "a", "b"));

		Optional<DoIt> found = doService.findById(saved.getNum());

		assertThat(found).isPresent();
		assertThat(found.get().getContent()).isEqualTo("b");
	}

	@Test
	void update_replaces_fields() {
		DoIt saved = doRepository.save(new DoIt(null, "old", "oldc"));

		Optional<DoIt> updated = doService.update(new DoDto(saved.getNum(), "new", "newc"));

		assertThat(updated).isPresent();
		assertThat(updated.get().getTitle()).isEqualTo("new");
		assertThat(doRepository.findById(saved.getNum())).hasValueSatisfying(e -> {
			assertThat(e.getTitle()).isEqualTo("new");
			assertThat(e.getContent()).isEqualTo("newc");
		});
	}

	@Test
	void update_when_num_null_returns_empty() {
		Optional<DoIt> result = doService.update(new DoDto(null, "x", "y"));

		assertThat(result).isEmpty();
	}

	@Test
	void delete_returns_true_and_removes() {
		DoIt saved = doRepository.save(new DoIt(null, "t", "c"));

		boolean deleted = doService.delete(saved.getNum());

		assertThat(deleted).isTrue();
		assertThat(doRepository.findById(saved.getNum())).isEmpty();
	}

	@Test
	void delete_unknown_id_returns_false() {
		assertThat(doService.delete(9999L)).isFalse();
	}
}
