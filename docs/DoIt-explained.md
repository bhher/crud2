# DoIt 엔티티 설명

`com.example.crud2.entity.DoIt`은 **JPA 엔티티**입니다. **DB 테이블 한 줄(한 행)** 과 대응되는 자바 객체로 이해하면 됩니다.

---

## 1. 전체 구조 한눈에 보기

```java
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
```

**한 줄로:** 이 클래스는 **DB 테이블과 1:1로 매핑**되는 객체입니다. `DoIt` ≈ “할 일(게시글) 한 건을 담는 테이블”이라고 보면 됩니다.

---

## 2. JPA 어노테이션

### `@Entity`

- JPA에게 **“이 타입은 영속성 대상이고, 테이블과 매핑된다”**고 알려 줍니다.
- **논리 이름**은 클래스명 `DoIt`에서 따옵니다.

**실제 물리 테이블 이름**은 Hibernate/Spring Boot **네이밍 전략**에 따라 달라질 수 있습니다.

- Spring Boot 기본 **물리 네이밍**을 쓰면 보통 **`do_it`** 처럼 스네이크 케이스로 생성되는 경우가 많습니다.
- 정확한 이름은 애플리케이션 기동 시 로그의 `create table ...` DDL을 보면 됩니다.

### `@Id`

- **기본키(Primary Key)** 컬럼에 붙입니다.
- 이 프로젝트에서는 `num`이 PK입니다.

```java
private Long num;
```

### `@GeneratedValue(strategy = GenerationType.IDENTITY)`

- PK 값을 **DB가 자동 생성**하도록 맡깁니다 (MySQL·H2 등에서 흔한 **auto increment / identity**).
- 저장 시 `num`에 `null`을 넣고 `save` 하면 DB가 `1, 2, 3, …` 식으로 채워 줍니다.

### `@Column`

- 필드가 **테이블의 컬럼**에 매핑됨을 나타냅니다.
- 단순 `String` 필드는 **`@Column`을 생략**해도 기본적으로 컬럼으로 매핑되는 경우가 많습니다.  
  이 소스에서는 명시해 두었을 뿐입니다.

---

## 3. Lombok 어노테이션

| 어노테이션 | 역할 |
|------------|------|
| `@Getter` | 모든 필드에 대한 **getter** 자동 생성 |
| `@NoArgsConstructor` | **인자 없는 생성자**. JPA는 프록시·리플렉션을 위해 **기본 생성자**가 필요한 경우가 많음 |
| `@AllArgsConstructor` | **모든 필드**를 받는 생성자 (테스트·서비스에서 `new DoIt(null, title, content)` 등으로 사용) |

---

## 4. 생성되는 테이블 구조 (개념)

엔티티에 대응하는 테이블은 대략 다음과 같은 형태입니다. (이름·타입은 DB·설정에 따라 조금 달라질 수 있음)

```sql
CREATE TABLE do_it (
    num BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    content VARCHAR(255)
);
```

H2/MySQL에서 `AUTO_INCREMENT`/`IDENTITY` 표현은 방언에 따라 문법이 다릅니다. **실제 DDL은 실행 로그를 기준**으로 확인하세요.

---

## 5. 데이터 저장 흐름 예시

```java
DoIt doit = new DoIt(null, "제목", "내용");
repository.save(doit);
```

- `num`은 `null`로 두고 저장 → DB가 **자동으로 번호** 부여.
- `title`, `content`는 지정한 문자열이 저장됨.

---

## 6. H2 / Spring Boot에서 초기 데이터 넣기

### 방법 1: `data.sql` (많이 씀)

**1단계:** `src/main/resources/data.sql` 파일을 만든다.

**2단계:** SQL 작성 (테이블명은 **실제 생성된 이름**에 맞출 것. 예: `do_it`)

```sql
INSERT INTO do_it (title, content) VALUES ('첫 번째 제목', '첫 번째 내용');
INSERT INTO do_it (title, content) VALUES ('두 번째 제목', '두 번째 내용');
```

**3단계:** Spring Boot가 스키마 생성 **뒤**에 스크립트를 실행하게 하려면 보통 아래를 함께 쓴다.

```properties
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

- `spring.jpa.hibernate.ddl-auto=create`(또는 `create-drop`)일 때, JPA가 먼저 테이블을 만들고 **그 다음** `data.sql`이 실행되도록 하려면 **`defer-datasource-initialization=true`** 가 중요하다.
- Spring Boot 2.5+ 에서는 `spring.sql.init.mode` 기본값이 바뀐 적이 있어, **`always`(또는 필요 시 `embedded`)** 로 명시하는 편이 안전하다.

> **crud2 현재 `application.properties`:** 위 속성은 기본으로 넣어져 있지 않다. `data.sql`을 쓰려면 추가한다.

### 방법 2: `import.sql`

- `src/main/resources/import.sql` 을 두고, **Hibernate 초기화**와 연동되는 방식(주로 `ddl-auto=create` 계열)에서 쓰이는 패턴이 있다.
- 프로젝트·버전에 따라 `data.sql`과 역할이 겹치므로, **한 가지 방식을 정해 통일**하는 것이 좋다.

### 방법 3: `CommandLineRunner` / `ApplicationRunner`

```java
@Bean
CommandLineRunner init(DoRepository repo) {
	return args -> {
		repo.save(new DoIt(null, "초기 제목1", "초기 내용1"));
		repo.save(new DoIt(null, "초기 제목2", "초기 내용2"));
	};
}
```

| 장점 | 설명 |
|------|------|
| Java 코드 | 조건 분기·프로파일별 실행 등 **로직**을 넣기 쉽다 |
| 타입 안전 | 컴파일 타임에 엔티티와 맞춰 쓸 수 있다 |

`@Configuration` 클래스에 두고, Repository를 주입해 사용한다.

---

## 7. 메모리 DB(`mem`) 쓸 때 초기 데이터

```properties
spring.datasource.url=jdbc:h2:mem:testdb
```

- 프로세스/연결이 끝나면 데이터가 **사라지기 쉽다**.
- 매 기동마다 비어 있는 DB에 **시드 데이터**를 넣으려면 `data.sql` + 위의 `defer` / `spring.sql.init.mode` 조합이나 `CommandLineRunner`가 잘 맞는다.

**자주 쓰는 조합 예:**

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

---

## 한 줄 정리

| 항목 | 의미 |
|------|------|
| `@Entity` | 이 클래스는 **테이블과 매핑**된다 |
| `@Id` | **기본키** |
| `@GeneratedValue` | PK **자동 증가** 등 DB 위임 |
| `data.sql` | **초기 데이터**를 넣는 대표적인 방법 (실행 순서는 `defer-datasource-initialization` 등과 함께 맞출 것) |

---

## 관련 파일

- 엔티티 소스: `src/main/java/com/example/crud2/entity/DoIt.java`
- 설정: `src/main/resources/application.properties`
- [application.properties 상세](./application-properties-explained.md)
