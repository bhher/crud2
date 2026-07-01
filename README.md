# crud2 프로젝트 설명

`crud1`(Mustache + 컨트롤러에서 Repository 직접 사용)을 기반으로 한 **Spring Boot 3** 예제입니다. **Thymeleaf**로 화면을 구성하고, **Service 계층**으로 비즈니스 로직을 분리했습니다.

## 기술 스택

| 구분 | 내용 |
|------|------|
| Java | 17 |
| 빌드 | Gradle (`crud2` 루트에 Wrapper 포함) |
| 프레임워크 | Spring Boot 3.3.12 |
| 웹 | Spring Web (MVC) |
| 뷰 | **Thymeleaf** (`spring.thymeleaf.cache=false` 개발 편의) |
| ORM | Spring Data JPA (Hibernate) |
| DB (기본 설정) | **H2** 파일 DB `~/test_crud2` |
| 기타 | Lombok |

`build.gradle`에는 `mysql-connector-j`도 포함되어 있어, `application.properties`만 MySQL URL/드라이버로 바꾸면 MySQL로 전환할 수 있습니다.

## 패키지 구조 (`com.example.crud2`)

```
com.example.crud2
├── Crud2Application.java      # 진입점
├── controller
│   └── DoController.java      # HTTP 요청·응답, 뷰 이름 반환
├── service
│   └── DoService.java         # 트랜잭션·CRUD 위임
├── repository
│   └── DoRepository.java      # JpaRepository
├── entity
│   └── DoIt.java              # JPA 엔티티 (테이블 매핑)
└── dto
    └── DoDto.java             # 폼/수정용 DTO (getter·setter)
```

- **Controller**는 `DoService`만 의존하고, Repository를 직접 쓰지 않습니다.
- **Service**는 조회 메서드에 `@Transactional(readOnly = true)`, 변경 메서드에 `@Transactional`을 둡니다.

## 도메인 모델

### 엔티티 `DoIt`

- `num` (Long, PK, 자동 증가)
- `title`, `content`

### DTO `DoDto`

- 작성/수정 폼과 스프링 MVC 바인딩용 (`title`, `content`, 수정 시 `num`)
- `toEntity()`로 `DoIt`로 변환 가능 (서비스에서는 생성 시 `num` 없이 새 엔티티를 만드는 방식도 사용)

## URL·화면 매핑

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/list` | 목록 (`mains/doList.html`) |
| GET | `/list/{num}` | 상세 (`mains/detail.html`) |
| GET | `/mains/add` | 등록 폼 (`mains/add.html`) |
| POST | `/mains/create` | 등록 처리 → 상세로 리다이렉트 |
| GET | `/list/{num}/edit` | 수정 폼 (`mains/edit.html`) |
| POST | `/mains/update` | 수정 처리 |
| GET | `/list/{num}/delete` | 삭제 후 `/list`로 리다이렉트 (플래시 메시지) |

Thymeleaf 템플릿은 `src/main/resources/templates/` 아래에 있습니다.

- `layouts/header.html`, `layouts/footer.html`: 공통 레이아웃 조각(`th:fragment`, `th:replace`)
- `mains/*.html`: 본문 페이지

목록 화면에서는 제목을 눌러 상세(`/list/{num}`)로 이동하도록 링크가 걸려 있습니다.

## 설정 파일

- `src/main/resources/application.properties`  
  - H2 드라이버, `jdbc:h2:~/test_crud2`, H2 콘솔 `/h2-console`
  - `spring.jpa.hibernate.ddl-auto=create` (기동 시 스키마 생성)

## 실행 방법

프로젝트 루트(`crud2`)에서:

```bash
.\gradlew.bat bootRun
```

브라우저에서 예: `http://localhost:8080/list` 로 목록을 열 수 있습니다.

테스트:

```bash
.\gradlew.bat test
```

## crud1과의 차이 요약

| 항목 | crud1 | crud2 |
|------|--------|--------|
| 템플릿 | Mustache | **Thymeleaf** |
| 데이터 접근 | Controller → Repository | Controller → **Service** → Repository |
| 삭제 후 이동 | 목록(뷰 이름만 반환하는 형태 등) | **`redirect:/list`** + 플래시 메시지 |
| DB 파일 | `~/test` (설정에 따름) | **`~/test_crud2`** (crud1과 분리) |

## 개발 시 참고

- 폼 바인딩을 쓰는 DTO(`DoDto`)에는 **getter/setter**가 필요합니다.
- IDE에서만 `main`을 실행할 때 `@PathVariable` 관련 오류가 나면, 컴파일 옵션 `-parameters` 사용 또는 `@PathVariable("num")`처럼 이름을 명시하는 방식을 고려하세요. (`firstproject`에서 적용한 방식과 동일)

---

이 문서는 `crud2` 폴더 기준으로 작성되었습니다.
