package com.example.crud2.service;

import com.example.crud2.dto.DoDto;
import com.example.crud2.entity.DoIt;
import com.example.crud2.repository.DoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DoService {

	private final DoRepository doRepository;

	public DoService(DoRepository doRepository) {
		this.doRepository = doRepository;
	}

	public List<DoIt> findAll() {
		return doRepository.findAll();
	}

	public Optional<DoIt> findById(Long num) {
		return doRepository.findById(num);
	}

	@Transactional
	public DoIt create(DoDto dto) {
		DoIt entity = new DoIt(null, dto.getTitle(), dto.getContent());
		return doRepository.save(entity);
	}

	@Transactional
	public Optional<DoIt> update(DoDto dto) {
		if (dto.getNum() == null) {
			return Optional.empty();
		}
		return doRepository.findById(dto.getNum()).map(existing -> {
			DoIt merged = new DoIt(dto.getNum(), dto.getTitle(), dto.getContent());
			return doRepository.save(merged);
		});
	}

	@Transactional
	public boolean delete(Long num) {
		return doRepository.findById(num)
				.map(entity -> {
					doRepository.delete(entity);
					return true;
				})
				.orElse(false);
	}
}
