# Thymeleaf 가이드 (Spring Boot + 예제)

[Thymeleaf](https://www.thymeleaf.org/)는 서버 사이드 HTML 템플릿 엔진이다. Spring MVC의 `Model`에 넣은 값을 HTML에 안전하게 꽂고, 링크·폼·반복·조각(레이아웃)을 다루는 데 쓴다. 공식 튜토리얼: [Using Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html).

---

## 1. 선언과 “자연 템플릿”

`html`에 Thymeleaf 네임스페이스를 선언한다.

```html
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
```

`th:*` 속성은 **서버에서 렌더링될 때** 적용된다. 브라우저가 파일을 그냥 열어도 `th:text` 옆의 **본문 텍스트**가 보이므로, 디자이너·기획과 협업하기 좋다(자연 템플릿).

---

## 2. 표현식 종류

| 문법 | 이름 | 용도 |
|------|------|------|
| `${...}` | 변수 표현식 | `Model` 속성, 세션 등 (`${detail.title}`) |
| `*{...}` | 선택 표현식 | `th:object`로 선택한 객체 기준 (`*{title}`) |
| `@{...}` | 링크(URL) 표현식 | 컨텍스트 경로·쿼리스트링 조합 (`@{/list/{n}(n=${id})}`) |
| `#{...}` | 메시지 표현식 | `messages.properties` 다국어 문구 |
| `~{...}` | 조각(fragment) 표현식 | 다른 템플릿의 `th:fragment` 참조 |

---

## 3. 출력: `th:text` / `th:utext`

- **`th:text`**: 이스케이프 처리(HTML 특수문자 안전). 일반적으로 이것만 쓴다.
- **`th:utext`**: HTML 그대로 출력. **신뢰할 수 없는 사용자 입력**에 쓰면 XSS 위험이 있다.

```html
<p th:text="${userName}">게스트</p>
```

컨트롤러에서 `model.addAttribute("userName", "Kim");`이면 결과는 `Kim`이다.

**Elvis 연산자** (`?:`): 값이 null이면 대체.

```html
<title th:text="${pageTitle} ?: 'Crud2'">Crud2</title>
```

(`crud2`의 `layouts/header.html`과 같은 패턴.)

---

## 4. 반복: `th:each`

컬렉션을 순회한다. `item`은 루프 변수, `stat`은 반복 상태(선택).

```html
<tr th:each="item : ${DoList}">
  <td th:text="${item.num}">1</td>
  <td th:text="${item.title}">제목</td>
</tr>
```

`DoList`는 컨트롤러에서 `model.addAttribute("DoList", list);`로 넘긴 이름과 같아야 한다.

---

## 5. 링크: `th:href`와 `@{...}`

`@{...}`는 애플리케이션 컨텍스트 경로를 자동으로 붙여 주고, 경로 변수·쿼리 파라미터를 안전하게 만든다.

```html
<!-- 경로 변수: /list/5 -->
<a th:href="@{/list/{n}(n=${item.num})}" th:text="${item.title}">title</a>

<!-- 여러 경로 변수 -->
<a th:href="@{/list/{id}/edit(id=${detail.num})}">수정</a>
```

정적 파일은 보통 `@{/css/style.css}`처럼 쓴다.

---

## 6. 조건: `th:if` / `th:unless`

표현식이 참이면 해당 태그(와 자식)가 렌더링된다.

```html
<table th:if="${detail != null}">
  ...
</table>
```

`th:unless`는 반대(거짓일 때만 출력).

---

## 7. 폼: `th:action`, `th:object`, `th:field`

- **`th:object`**: 폼이 바인딩할 **단일 객체**(DTO/엔티티)를 지정.
- **`th:field="*{프로퍼티}"`**: `name`, `value`, (필요 시) `id`를 자동 생성. 검증 오류 시 값 유지에도 유리.

```html
<form th:action="@{/mains/update}" th:object="${editDto}" method="post">
  <input type="hidden" th:field="*{num}">
  <input type="text" th:field="*{title}">
  <textarea th:field="*{content}"></textarea>
  <button type="submit">저장</button>
</form>
```

등록 폼은 `th:object="${doDto}"`처럼 빈 DTO를 넘기고, 수정 폼은 기존 값이 채워진 DTO를 넘긴다(`crud2`의 `add.html`, `edit.html`).

---

## 8. 레이아웃 조각: `th:fragment` / `th:replace`

**정의**(조각을보내는 템플릿):

```html
<head th:fragment="head">
  <meta charset="UTF-8">
  <title>앱</title>
</head>
```

**사용**(호출하는 페이지에서 통째로 교체):

```html
<head th:replace="~{layouts/header :: head}"></head>
```

- `~{템플릿경로 :: 프래그먼트이름}` 형식이다.
- `th:replace`: **자기 태그를 조각으로 바꾼다**(자식도 함께 대체).
- `th:insert`: 조각을 **안쪽에 삽입**한다.

네비·푸터를 공통으로 두는 패턴이 `crud2`의 `doList.html`, `detail.html` 등에 적용되어 있다.

---

## 9. 이 crud2 프로젝트에서의 흐름 (요약)

1. **목록** (`doList.html`): `th:each`로 `DoList` 순회, 상세 링크는 `@{/list/{n}(n=${item.num})}`.
2. **상세** (`detail.html`): `detail`이 있을 때만 `th:if`로 테이블·버튼 표시, 수정/삭제 URL에 `detail.num` 전달.
3. **등록/수정**: `th:object` + `th:field`로 POST 바인딩, 액션은 각각 `@{/mains/create}`, `@{/mains/update}`.

---

## 10. 자주 쓰는 팁

- **IDE**: IntelliJ Ultimate는 Thymeleaf 지원이 좋다. `Model` 키 자동완성은 제한적이므로 이름 오타에 주의.
- **캐시**: 개발 중에는 `spring.thymeleaf.cache=false`로 두면 수정이 바로 반영된다(`application.properties` 참고).
- **디버그**: 임시로 `[[${변수}]]` 인라인(프로세싱)을 쓸 수 있으나, 본문 이스케이프 규칙을 이해한 뒤 사용한다.

---

## 11. 한 줄 체크리스트

- 출력은 기본 **`th:text`**.
- URL은 **`@{...}`**.
- 폼은 **`th:object` + `th:field`**.
- 반복은 **`th:each`**, 분기는 **`th:if`**.
- 공통 UI는 **`th:fragment` + `th:replace`**.

이 문서는 `crud2` 모듈의 템플릿 구조를 기준으로 작성되었다.
