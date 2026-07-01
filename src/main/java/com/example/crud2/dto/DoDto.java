package com.example.crud2.dto;

import com.example.crud2.entity.DoIt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DoDto {

	private Long num;
	private String title;
	private String content;

	public DoIt toEntity() {
		return new DoIt(num, title, content);
	}
}
