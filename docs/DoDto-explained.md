# DoDto 클래스 설명

`com.example.crud2.dto.DoDto`는 Spring Boot에서 **DTO(Data Transfer Object)** 역할을 하는 클래스입니다.

---

## 전체 구조 한눈에 보기

```java
public class DoDto {
    private Long num;
    private String title;
    private String content;

    public DoIt toEntity() {
        return new DoIt(num, title, content);
    }
}
```

**핵심 역할:** 데이터를 담아 전달하는 객체이며, 필요 시 **Entity로 변환**하는 역할을 합니다.

---

## 1. DTO란?

**DTO(Data Transfer Object)** 는 화면(Controller) ↔ Service ↔ DB(Entity) 사이에서 **데이터를 옮기는 객체**입니다.

**왜 쓰나요?**

- Entity를 그대로 노출하면 보안·구조 측면에서 부담이 될 수 있음
- **필요한 필드만** 전달할 수 있음
- 화면/API 스펙이 바뀌어도 Entity와 분리해 **유지보수**하기 쉬움

---

## 2. Lombok 어노테이션

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
```

Lombok이 아래와 같은 보일러플레이트 코드를 **자동 생성**합니다.

| 어노테이션 | 설명 |
|------------|------|
| `@Getter` | 각 필드에 대한 getter 생성 |
| `@Setter` | 각 필드에 대한 setter 생성 |
| `@NoArgsConstructor` | 인자 없는 기본 생성자 생성 |
| `@AllArgsConstructor` | 모든 필드를 받는 생성자 생성 |
| `@ToString` | `toString()` 메서드 생성 |

즉, 다음을 직접 쓰지 않아도 됩니다.

```java
public String getTitle() { return title; }
public void setTitle(String title) { this.title = title; }
// …
```

> **참고:** Spring MVC가 폼이나 쿼리 파라미터를 객체에 넣을 때는 **기본 생성자 + setter**가 필요합니다. 그래서 `@NoArgsConstructor`와 `@Setter`가 중요합니다.

---

## 3. 필드 설명

```java
private Long num;
private String title;
private String content;
```

게시글(또는 할 일) 한 건에 해당하는 데이터로 보면 됩니다.

| 필드 | 의미 |
|------|------|
| `num` | 글 번호 (DB에서는 보통 PK에 대응) |
| `title` | 제목 |
| `content` | 내용 |

---

## 4. 핵심: `toEntity()`

```java
public DoIt toEntity() {
    return new DoIt(num, title, content);
}
```

**DTO → Entity** 로 바꿔 주는 메서드입니다.

**왜 필요할까?**

- **DTO**는 전달·표현용 데이터 묶음
- **Entity**는 JPA가 DB 테이블과 매핑하는 객체

저장·수정처럼 DB와 맞닿는 작업은 **Entity**로 수행하는 경우가 많습니다.

**흐름 (중요)**

```text
사용자 입력 → DTO → Entity → DB 저장
```

예시:

```java
DoDto dto = new DoDto(1L, "제목", "내용");

// Entity로 변환
DoIt entity = dto.toEntity();

// 저장
repository.save(entity);
```

> 이 프로젝트의 `DoService#create`에서는 새 글일 때 `num` 없이 `new DoIt(null, title, content)` 로 만드는 식으로 쓰기도 합니다. `toEntity()`는 수정·일반 변환 등 다른 경로에서 재사용하기 좋습니다.

---

## 5. `DoIt` 클래스는?

`return new DoIt(num, title, content);` 에서 **`DoIt`은 Entity 클래스**입니다.

개념적으로는 다음과 비슷합니다.

```java
@Entity
public class DoIt {
    private Long num;
    private String title;
    private String content;
}
```

실제 소스는 `com.example.crud2.entity.DoIt` 을 참고하면 됩니다.

---

## 6. DTO 안에 변환 메서드를 두는 이유

- 코드가 **간결**해짐
- DTO ↔ Entity 변환 규칙을 **한곳**에서 관리
- 여러 Controller/Service에서 **재사용**하기 쉬움

별도 `Mapper` 클래스로 빼는 방식도 많이 쓰며, 규모가 커지면 그쪽이 더 낫다고 판단할 수 있습니다.

---

## 한 줄 정리

**`DoDto`는 데이터를 담는 객체이고, `toEntity()`로 DB 저장·영속성 계층에서 쓰는 `DoIt` Entity로 바꿀 수 있다.**
