# application.properties 설정 설명 (crud2)

`src/main/resources/application.properties`에 들어 있는 항목을 정리한 문서입니다.

---

## H2 데이터베이스란?

**H2**는 **Java로 구현된 오픈소스 관계형 DB(RDBMS)** 입니다.

| 특징 | 설명 |
|------|------|
| 용량 | 약 1.7MB 수준으로 **가볍다** |
| 실행 환경 | **JVM 위에서 동작**하므로, 사용하는 방식에 따라 Java(또는 Java 기반 앱)가 갖춰져 있어야 한다 |
| **임베디드 모드** | 애플리케이션 **JVM 메모리 안**에서 DB가 같이 뜨는 방식 (Spring Boot + H2가 대표적) |
| **서버 모드** | 일반 DB처럼 **별도 프로세스(서버)** 로 띄우고, 클라이언트가 TCP 등으로 접속하는 방식 |
| 활용 | **로컬 개발**, **테스트**, 프로토타입 등에서 자주 쓰인다 |

> **crud2 프로젝트:** Gradle 의존성으로 H2 드라이버를 포함하고, 애플리케이션과 **같이 기동되는 임베디드/인프로세스** 형태로 쓰는 경우가 많다. 아래 “독립 설치”는 **선택 사항**이다.

---

## H2 데이터베이스 독립 설치 (선택)

애플리케이션에 끼워 넣지 않고, **H2만 단독으로 서버 모드**로 쓰고 싶을 때의 절차 요약이다.

1. [H2 Database Engine](https://www.h2database.com/)에서 **Windows Installer** 또는 **All Platforms(zip)** 를 받는다.
2. 설치하거나 압축을 푼다.
3. **Windows** 기준 설치/압축 폴더에서 **`h2.bat`** 또는 **`h2w.bat`** 을 실행한다.  
   → H2가 **서버 모드**로 기동된다.
4. 웹 브라우저에서 **`http://localhost:8082`** 로 접속해 콘솔을 연다.  
   (기본 포트는 환경에 따라 다를 수 있으니, 실행 시 안내 메시지를 확인한다.)

**Spring Boot 내장 H2 콘솔과의 차이**

| 구분 | 독립 H2 (설치본) | Spring Boot + `spring.h2.console` |
|------|------------------|-------------------------------------|
| 기동 | `h2.bat` / `h2w.bat` 등 | 스프링 앱(`bootRun`)과 함께 |
| 콘솔 URL 예 | `http://localhost:8082` | `http://localhost:8080/h2-console` (앱 포트 따름) |

출처·참고: [nyximos.log 티스토리 글](https://nyximos.tistory.com/73)

---

## 1. 전체 설정 요약

### 애플리케이션 이름

```properties
spring.application.name=crud2
```

- 스프링 부트 애플리케이션 **식별 이름**입니다.
- 로그, Actuator, 일부 클라우드/모니터링 도구에서 구분할 때 쓰입니다.

---

### 데이터베이스 연결 (H2, 파일 방식)

```properties
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.url=jdbc:h2:~/test_crud2
spring.datasource.username=sa
spring.datasource.password=
```

| 항목 | 설명 |
|------|------|
| `driver-class-name` | JDBC 드라이버로 **H2** 사용 |
| `url` | DB 접속 URL. **`~/test_crud2`** 는 사용자 **홈 디렉터리**에 파일 DB 생성 |
| `username` / `password` | H2 기본 계정 `sa`, 비밀번호 비움 |

**의미:** 디스크에 파일로 저장되는 **영속 H2 DB**입니다. 애플리케이션을 껐다 켜도 (파일이 남아 있으면) 데이터가 이어질 수 있습니다.

**파일 위치 예 (Windows):**

`C:\Users\<사용자명>\test_crud2.mv.db`

---

### H2 콘솔

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- 브라우저에서 H2에 접속해 테이블·데이터를 확인할 수 있습니다.
- 앱 기동 후 주소: `http://localhost:8080/h2-console`
- **JDBC URL** 입력란에는 `application.properties`와 **동일한** URL을 넣어야 합니다.  
  예: `jdbc:h2:~/test_crud2`

---

### JPA / Hibernate

```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

| 항목 | 설명 |
|------|------|
| `database-platform` | H2에 맞는 SQL/타입 처리를 쓰겠다는 설정 |
| `ddl-auto=create` | 기동 시 스키마를 **새로 생성**. 기존 테이블이 있으면 드롭 후 생성되는 동작에 가깝게 동작할 수 있어 **기존 데이터는 유지되지 않을 수 있음** |
| `show-sql` | 실행되는 SQL을 로그로 출력 |
| `format_sql` | 로그에 찍히는 SQL을 읽기 쉽게 줄바꿈 |

> `ddl-auto=create`와 파일 DB를 같이 쓰면 “파일은 남아 있어도 매번 스키마/데이터가 갈아엎일 수 있다”는 점을 같이 이해하는 것이 좋습니다. 데이터를 오래 남기려면 `update` 등으로 바꾸는 방안을 검토합니다.

---

### Thymeleaf

```properties
spring.thymeleaf.cache=false
```

- 개발 중에는 템플릿 **캐시를 끄면** HTML 수정 후 재시작 없이 반영되기 쉽습니다.
- 운영 배포 시에는 보통 `true`로 두어 성능을 올리는 경우가 많습니다.

---

## 2. 파일 DB (`~/test_crud2`) 방식

**현재 crud2 기본 설정:**

```properties
spring.datasource.url=jdbc:h2:~/test_crud2
```

| 특징 | 설명 |
|------|------|
| 저장 위치 | 사용자 홈 아래 **디스크 파일** |
| 재시작 후 | 파일이 남아 있으면 DB 파일도 남음 (`ddl-auto`에 따라 테이블/데이터는 다시 만들어질 수 있음) |
| 용도 | 로컬에서 데이터를 잠깐 쌓아 두고 확인할 때 |

H2 콘솔 접속 시 **JDBC URL**은 위와 동일하게 입력합니다.

### `jdbc:h2:~/test_crud2` vs `jdbc:h2:tcp://localhost/~/test_crud2`

둘 다 **같은 이름의 파일 DB(`~/test_crud2`)** 를 가리킬 수 있지만, **연결 방식**이 다릅니다.

```properties
# 앱 JVM 안에서 직접 파일 DB 열기 (임베디드)
spring.datasource.url=jdbc:h2:~/test_crud2

# 로컬에서 떠 있는 H2 서버에 TCP로 접속 (서버가 홈 디렉터리의 동일 DB를 연 상태)
spring.datasource.url=jdbc:h2:tcp://localhost/~/test_crud2
```

| 항목 | `jdbc:h2:~/test_crud2` | `jdbc:h2:tcp://localhost/~/test_crud2` |
|------|------------------------|------------------------------------------|
| **연결 방식** | **임베디드(인프로세스)**. Spring 앱이 돌아가는 **같은 JVM** 안에서 H2가 DB 파일을 연다 | **TCP 클라이언트**. **별도 프로세스로 기동한 H2 서버**(`h2.bat` 서버 모드 등)에 접속한다 |
| **H2 서버** | 따로 띄울 필요 없음 (의존성만 있으면 앱과 함께 동작) | **`localhost`에 H2 서버가 먼저 실행** 중이어야 한다 |
| **`~/` 의 기준** | **애플리케이션을 실행한 OS 사용자**의 홈 디렉터리 | **H2 서버 프로세스**가 사용하는 홈 디렉터리 (보통 같은 PC면 위와 동일) |
| **여러 프로그램 동시 접근** | 같은 파일을 **다른 JVM이 동시에 임베디드로 열면** 잠금/충돌 이슈가 나기 쉽다 | 서버가 파일을 한 번 열고 클라이언트만 여러 개 붙을 수 있어, **콘솔 + 앱** 등 동시 접속에 유리한 편이다 |
| **crud2 기본값** | ✅ `application.properties`에 이 형태가 들어 있다 | 서버 모드 H2를 쓸 때만 설정을 이렇게 바꾼다 |

**정리**

- **`~/test_crud2`만 있는 URL** → “앱 안에 붙은 H2”로 **로컬 파일**을 연다.
- **`tcp://localhost/...`** 가 붙은 URL → “이미 떠 있는 H2 **서버**”에 붙고, 서버가 **`~/test_crud2` 파일**을 담당한다.

포트를 쓰려면 예: `jdbc:h2:tcp://localhost:9092/~/test_crud2` — H2 TCP 기본 포트는 환경마다 다를 수 있으니, 서버 기동 시 콘솔에 나오는 주소를 따른다.

---

## 3. 메모리 DB (`mem`) 방식

### 설정 예시

```properties
spring.datasource.url=jdbc:h2:mem:testdb
```

| 항목 | 설명 |
|------|------|
| 저장 위치 | RAM(메모리) |
| 프로세스 종료 | DB 내용이 사라지는 것이 일반적 |
| 속도 | 디스크보다 빠른 편 |
| 용도 | 단위 테스트, 임시 개발 |

**콘솔 접속:** 앱이 사용 중인 JVM과 **같은** 메모리 DB 이름으로 붙어야 합니다.  
예: `jdbc:h2:mem:testdb`

### 연결이 끊겨도 메모리를 잠깐 유지하고 싶을 때

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
```

- 마지막 연결이 닫혀도 DB를 바로 날리지 않도록 하는 옵션입니다.  
- 그래도 **애플리케이션 프로세스 자체가 종료**되면 메모리 DB는 사라집니다.

---

## 4. 파일 DB vs 메모리 DB 비교

| 항목 | 파일 DB (`~/…`) | 메모리 DB (`mem:…`) |
|------|-----------------|---------------------|
| 저장 위치 | 디스크 | RAM |
| 데이터 유지 | 파일 기준으로 상대적 유지 | 프로세스/연결에 따라 대부분 휘발 |
| 속도 | 보통 | 매우 빠른 편 |
| 전형적 용도 | 로컬 개발·데이터 남겨 보기 | 테스트·빠른 반복 |

---

## 5. 상황별 추천 (참고)

- **빠르게 반복·초기화:** 메모리 DB (`mem`) + 필요 시 `DB_CLOSE_DELAY=-1`
- **파일로 남기며 확인:** `jdbc:h2:~/test_crud2` (현재 프로젝트와 동일한 방식)
- **스키마/데이터를 오래 유지:** `ddl-auto`를 `create`만 쓰지 말고 `update` 등과 조합해 검토

---

## 6. 설정 예시 한눈에

**메모리 DB**

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**파일 DB (crud2 기본과 동일한 패턴)**

```properties
spring.datasource.url=jdbc:h2:~/test_crud2
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

---

## 한 줄 요약

- **`jdbc:h2:~/test_crud2`** → 홈 디렉터리에 **파일**로 저장 (디스크 기반).
- **`jdbc:h2:mem:…`** → **메모리** 저장, 앱/프로세스 종료 시 대부분 사라짐.

---

## 현재 저장소의 `application.properties` 원문

설정이 바뀌면 아래 파일을 기준으로 확인하세요.

- `crud2/src/main/resources/application.properties`
