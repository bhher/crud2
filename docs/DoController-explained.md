# DoController 클래스 설명

`com.example.crud2.controller.DoController`는 **웹 요청(URL)을 받아 Service를 호출하고, Thymeleaf 화면으로 연결**하는 MVC의 **Controller** 계층입니다.

---

## 전체 소스 코드

```java
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
```

---

## 한 줄 요약

웹 요청(URL)을 받아 **Service만 호출**하고, 처리 결과를 **Model에 담아 Thymeleaf 뷰 이름**을 반환하거나 **`redirect:`** 로 이동시킨다.

---

## 1. 클래스 선언과 `@Controller`

```java
@Controller
public class DoController {
```

| 항목 | 설명 |
|------|------|
| `@Controller` | 웹 요청을 처리하는 스프링 빈. **뷰 이름(문자열)** 을 반환하면 Thymeleaf 등 ViewResolver가 HTML을 찾음. |
| `@RestController`와 차이 | `@RestController`는 기본적으로 JSON 등 **HTTP 본문**에 직접 씀. 화면 템플릿을 쓸 때는 `@Controller`가 일반적. |

---

## 2. Service 주입 (생성자 DI)

```java
private final DoService doService;

public DoController(DoService doService) {
	this.doService = doService;
}
```

- **생성자 주입:** 스프링이 `DoService` 빈을 넣어 줌. `final`로 불변 참조 유지 가능.
- Controller는 **Repository/DB에 직접 접근하지 않고** Service만 사용하는 구조가 유지보수에 유리함.

```text
Controller → Service → Repository → DB
```

---

## 3. 글 작성 폼 화면

```java
@GetMapping("/mains/add")
public String addForm(Model model) {
	model.addAttribute("doDto", new DoDto());
	return "mains/add";
}
```

| 항목 | 설명 |
|------|------|
| URL | `GET /mains/add` |
| `Model` | 뷰로 넘길 **속성(map)**. 키 `"doDto"`로 빈 `DoDto`를 넣음. |
| 빈 DTO | Thymeleaf `th:object="${doDto}"` 로 폼과 양방향 바인딩하기 좋음. |
| 반환값 | `"mains/add"` → `templates/mains/add.html` |

---

## 4. 상세 보기 (READ, 단건)

```java
@GetMapping("/list/{num}")
public String detail(@PathVariable Long num, Model model) {
	return doService.findById(num)
			.map(doIt -> {
				model.addAttribute("detail", doIt);
				return "mains/detail";
			})
			.orElse("redirect:/list");
}
```

- URL 예: `GET /list/1` → `num` = `1`
- `@PathVariable`: URL 경로의 `{num}` 값을 메서드 인자로 받음.

**Optional 처리**

| 경우 | 동작 |
|------|------|
| `findById`에 값이 있음 | `detail`을 모델에 넣고 `"mains/detail"` 뷰 |
| 없음 | `"redirect:/list"` 로 목록으로 이동 |

이 프로젝트는 `@PathVariable("num")` 으로 이름을 명시하고, Gradle 컴파일에 `-parameters` 도 켜 두어 IDE 단독 실행 시에도 동일하게 동작하도록 맞춰 두었다.

---

## 5. 전체 목록 (READ, 목록)

```java
@GetMapping("/list")
public String list(Model model) {
	List<DoIt> doList = doService.findAll();
	model.addAttribute("DoList", doList);
	return "mains/doList";
}
```

- `GET /list`: 전체 조회 후 `"DoList"` 키로 모델에 추가.
- 뷰: `templates/mains/doList.html`에서 `th:each` 등으로 순회.

---

## 6. 수정 폼 화면

```java
@GetMapping("/list/{num}/edit")
public String updateForm(@PathVariable Long num, Model model) {
	return doService.findById(num)
			.map(toDo -> {
				model.addAttribute("editDto", new DoDto(toDo.getNum(), toDo.getTitle(), toDo.getContent()));
				return "mains/edit";
			})
			.orElse("redirect:/list");
}
```

- **Entity → DTO:** 화면/폼에는 `DoDto`를 쓰기 위해 `DoIt`에서 필드를 꺼내 `DoDto` 생성.
- 없는 `num`이면 역시 목록으로 리다이렉트.

---

## 7. 삭제

```java
@GetMapping("/list/{num}/delete")
public String delete(@PathVariable Long num, RedirectAttributes rttr) {
	if (doService.delete(num)) {
		rttr.addFlashAttribute("msg", "삭제가 완료되었습니다.");
	}
	return "redirect:/list";
}
```

- `RedirectAttributes.addFlashAttribute`: **리다이렉트 직후 한 번만** 모델에 담기는 값(세션 플래시). 새로고침 시 메시지가 반복되지 않게 할 때 자주 사용.
- 뷰에서는 `${msg}` 로 표시 가능(레이아웃에 `th:if="${msg}"` 등).

---

## 8. 글 등록 (CREATE)

```java
@PostMapping("/mains/create")
public String create(DoDto dto) {
	DoIt saved = doService.create(dto);
	return "redirect:/list/" + saved.getNum();
}
```

| 단계 | 설명 |
|------|------|
| 폼 POST | `name="title"`, `name="content"` 등이 `DoDto`의 setter로 **자동 바인딩** |
| Service | DTO를 받아 Entity로 저장 로직 수행 |
| 응답 | 저장된 PK(`num`)로 **상세 URL**로 리다이렉트 |

---

## 9. 글 수정 (UPDATE)

```java
@PostMapping("/mains/update")
public String update(DoDto dto) {
	return doService.update(dto)
			.map(saved -> "redirect:/list/" + saved.getNum())
			.orElse("redirect:/list");
}
```

| 경우 | 동작 |
|------|------|
| 수정 성공 (`Optional`에 값 있음) | 해당 글 상세로 리다이렉트 |
| 실패 (예: 잘못된 `num`) | 목록으로 리다이렉트 |

수정 폼에서는 보통 hidden으로 `num`을 같이 보냄.

---

## 전체 요청 흐름

```text
[사용자 요청]
      ↓
Controller (DoController)
      ↓
Service (DoService)
      ↓
Repository → DB
      ↓
Controller (결과를 Model에 담거나 redirect URL 결정)
      ↓
View (Thymeleaf) 또는 redirect 응답
```

---

## 자주 쓰는 매핑 정리

| 구분 | 설명 |
|------|------|
| `@GetMapping` | 조회, 폼 화면 열기 |
| `@PostMapping` | 폼 제출 등 **데이터 변경** 처리 |
| `@PathVariable` | `/list/{num}` 처럼 **경로 변수** 수신 |
| `Model` | 뷰로 넘길 **데이터** |
| `redirect:` 접두사 | 브라우저 **재요청**으로 다른 URL 이동 (POST 후 PRG 패턴에도 사용) |

---

## 관련 문서

- [DoDto 설명](./DoDto-explained.md)
- [프로젝트 개요](../README.md)
