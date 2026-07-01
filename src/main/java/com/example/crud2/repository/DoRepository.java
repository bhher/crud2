package com.example.crud2.repository;

import com.example.crud2.entity.DoIt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoRepository extends JpaRepository<DoIt, Long> {

	@Override
	List<DoIt> findAll();
}
