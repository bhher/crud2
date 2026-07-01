# crud2 — Spring Boot Docker 이미지 만들기

`crud2` 프로젝트는 **루트의 `Dockerfile`** 한 개로 이미지를 만든다. 로컬에 JDK·Gradle을 설치하지 않아도 되도록, **이미지 안에서 Gradle로 `bootJar`를 실행**하는 **멀티 스테이지 빌드**다.

---

## 구성 요약

| 항목 | 내용 |
|------|------|
| Spring Boot | 3.3.x |
| Java | 17 (Eclipse Temurin) |
| 빌드 스테이지 | `eclipse-temurin:17-jdk-alpine` — 소스 복사 후 `./gradlew bootJar` |
| 실행 스테이지 | `eclipse-temurin:17-jre-alpine` — `app.jar`만 포함 (이미지 크기 축소) |
| 포트 | `8080` |

---

## Dockerfile이 하는 일

1. **빌드 스테이지**  
   - `gradlew`, `build.gradle`, `src` 등을 복사한다.  
   - `./gradlew bootJar --no-daemon -x test` 로 실행 가능한 fat JAR를 만든다.  
   - Spring Boot Gradle 플러그인은 `*-plain.jar`와 **실행용 JAR**를 둘 다 만든다. 실행용만 쓰기 위해 **`plain`이 아닌 JAR 하나**를 골라 `app.jar`로 복사한다.

2. **실행 스테이지**  
   - 빌드 스테이지에서 만든 `app.jar`만 복사한다.  
   - 기본 엔트리포인트: `java $JAVA_OPTS -jar app.jar` (`JAVA_OPTS`는 비어 있어도 됨).

프로젝트의 실제 내용은 저장소 루트 `Dockerfile`을 보면 된다.

---

## 사전 준비

- **Docker Desktop**(Windows/Mac) 또는 Docker Engine + BuildKit 사용 가능 환경  
- 소스는 `crud2` 디렉터리 기준으로 두고, 아래 명령도 **`crud2` 루트**에서 실행한다.

---

## 이미지 빌드

```bash
cd crud2
docker build -t crud2:latest .
```

- 태그 `crud2:latest`는 예시이며, Docker Hub에 올릴 때는 `사용자명/crud2:latest` 형태로 태그를 추가하면 된다.  
- 로컬에서 `./gradlew bootJar`를 먼저 돌릴 필요는 **없다**. (Dockerfile이 컨테이너 안에서 빌드한다.)

### JVM 옵션 (선택)

이미지는 `JAVA_OPTS` 환경 변수를 JVM 앞에 붙인다. 메모리 제한 예:

```bash
docker run -e JAVA_OPTS="-Xms128m -Xmx256m" -p 8080:8080 crud2:latest
```

이미지 다시빌드
```bash
cd d:\spring1\crud2
docker build -t crud2:latest .
docker rm -f crud2-app 2>nul
docker run -e JAVA_OPTS="-Xms128m -Xmx256m" -p 8080:8080 --name crud2-app crud2:latest
```


---

## 실행 방법

### 1) docker-compose (권장 — 로컬 H2 + 볼륨)

`docker-compose.yml`은 이미지를 빌드하고 `SPRING_PROFILES_ACTIVE=docker`로 띄운다.  
`application-docker.properties`에서 H2 파일 DB 경로를 `/data/crud2-db`로 두고, **named volume**으로 데이터를 유지한다.

```bash
cd crud2
docker compose up -d --build
```

- 브라우저: `http://localhost:8080/list`  
- 중지: `docker compose down` (볼륨은 유지하려면 기본 설정 그대로 두면 됨)

### 2) docker run (프로파일만 지정)

`docker` 프로파일로 H2 파일 DB를 쓰려면 `/data`를 쓰기 가능하게 두는 것이 안전하다.

```bash
docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  crud2:latest
```

데이터를 호스트에 남기려면 `-v 호스트경로:/data` 를 추가한다.

### 3) RDS(MySQL) — 운영·EC2용

DB는 MySQL(RDS)이고 Spring은 `prod` 프로파일일 때 `application-prod.properties`의 환경 변수를 쓰도록 맞춰 두었다면, 예시는 다음과 같다. (값은 본인 RDS 정보로 바꾼다.)

```bash
docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://RDS엔드포인트:3306/crud2?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD='비밀번호' \
  crud2:latest
```

EC2에서 80번으로 노출하는 등의 흐름은 `docs/spring-boot-docker-aws-guide.md`를 참고한다.

---

## 의존성과 프로파일 정리

- **`build.gradle`**: `h2`, `mysql-connector-j` 모두 **runtime**으로 들어 있다. Docker 이미지에는 둘 다 포함되므로, **프로파일·설정**으로 H2 vs MySQL을 고른다.  
- **기본 `application.properties`**: 로컬 IDE용 H2(인메모리/홈 경로 등).  
- **`application-docker.properties`**: `docker` 프로파일 — 컨테이너 내 `/data` H2 파일 DB.  
- **`application-prod.properties`**: 운영/RDS — 보통 환경 변수로 URL·계정을 넘긴다.

---

## 자주 겪는 이슈

| 증상 | 원인·조치 |
|------|-----------|
| H2 Console: `remote connections ('webAllowOthers') are disabled` | 호스트 브라우저 → 컨테이너는 H2 입장에서 **원격**이다. `spring.h2.console.settings.web-allow-others=true` 사용(`application-docker.properties`에 있음). 설정을 바꾼 뒤에는 **이미지를 다시 빌드**해야 JAR에 반영된다. |
| `Cannot load driver class: org.h2.Driver` (또는 MySQL) | `build.gradle`에 해당 **runtime** 의존성이 있는지 확인 후 이미지 **재빌드** (`docker build --no-cache`). |
| `plain` JAR만 실행됨 | 이 프로젝트 Dockerfile은 `grep -v plain`으로 실행용 JAR만 고른다. 커스텀 Dockerfile을 쓸 때는 동일하게 처리할 것. |
| 포트 충돌 | 이미 8080을 쓰는 프로세스가 있으면 `-p 8081:8080` 등으로 바꾼다. |

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `Dockerfile` | 멀티 스테이지 빌드 + JRE 실행 이미지 |
| `docker-compose.yml` | 로컬에서 `docker` 프로파일 + H2 볼륨 |
| `build.gradle` | Java 17, Spring Boot, H2·MySQL 런타임 |
| `src/main/resources/application-docker.properties` | Docker 전용 H2 경로 |

AWS 전체 배포 흐름은 `docs/spring-boot-docker-aws-guide.md`, 상세 트러블슈팅은 `docs/deployment-full-journey.md`를 보면 된다.
