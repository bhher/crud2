package com.example.crud2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DoIt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long num;

	@Column
	private String title;

	@Column
	private String content;
}
