# crud2 — Docker로 배포하기 (처음부터)

이 문서는 **crud2**(Spring Boot 3 + Thymeleaf + JPA + H2) 프로젝트를 **Docker 이미지로 빌드**하고 **`docker compose`로 실행**하는 과정을 처음부터 순서대로 설명합니다.

---

## 1. 전제: 무엇을 하게 되나요?

| 단계 | 내용 |
|------|------|
| **Docker** | 애플리케이션을 “이미지”로 묶어, PC나 서버 어디서나 같은 환경으로 실행합니다. |
| **Dockerfile** | “JDK로 JAR를 만들고 → JRE만 담은 작은 이미지에 넣는다”는 **레시피** 파일입니다. |
| **docker-compose.yml** | 컨테이너(여기서는 `crud2` 앱 하나)를 **한 번에 빌드·실행**하고, 포트·환경변수·볼륨을 정합니다. |

로컬에서 `./gradlew bootRun`으로 돌리던 것을, **컨테이너 안에서 `java -jar`로 실행**한다고 이해하면 됩니다.

---

## 2. 준비물

1. **Docker Desktop** (Windows)  
   - [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/) 설치 후, 트레이 아이콘이 **Running**인지 확인합니다.  
   - WSL 2 백엔드를 쓰는 경우가 많습니다. 설치 마법사 안내를 따르면 됩니다.

2. **터미널**  
   - PowerShell, CMD, 또는 WSL 터미널 중 하나.

3. **프로젝트 위치**  
   - 이 가이드는 저장소 기준 **`crud2` 폴더 루트**에서 명령을 실행한다고 가정합니다.  
   - 예: `d:\spring1\crud2`

---

## 3. 로컬 설정과 Docker 설정의 차이 (중요)

### 로컬 `application.properties`

- H2 URL: `jdbc:h2:~/test_crud2` → 사용자 **홈 디렉터리** 아래에 DB 파일이 생깁니다.
- `ddl-auto=create` → 기동할 때마다 스키마를 새로 만듭니다.

### Docker 전용 `application-docker.properties`

컨테이너 안에는 “윈도우 홈 폴더” 개념이 애매하므로, 프로젝트에 **`spring.profiles.active=docker`** 일 때만 읽는 설정을 두었습니다.

- H2 URL: `jdbc:h2:file:/data/crud2-db` → 컨테이너 내부 경로 `/data`에 파일 DB를 둡니다.
- `docker-compose.yml`에서 **`/data`에 이름 있는 볼륨**을 붙이면, 컨테이너를 지워도 **데이터를 유지**할 수 있습니다.
- `ddl-auto=update` → 이미 만든 테이블은 유지하면서 스키마를 맞춥니다 (재기동 시 매번 초기화하지 않음).
- Thymeleaf `cache=true` → 운영에 가깝게 캐시 사용.

`docker-compose.yml`에서 다음 한 줄로 이 프로파일을 켭니다.

```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
```

---

## 4. 프로젝트에 포함된 파일 (역할만 짧게)

| 파일 | 역할 |
|------|------|
| **`Dockerfile`** | 멀티 스테이지: Gradle로 `bootJar` 빌드 → 실행용 이미지에는 **fat JAR 하나**만 복사. |
| **`docker-compose.yml`** | 이미지 빌드, 포트 `8080`, 프로파일 `docker`, H2 데이터 볼륨. |
| **`.dockerignore`** | `build/`, `.gradle/` 등 불필요한 파일을 빌드 컨텍스트에서 제외 (빌드 속도·용량 개선). |
| **`application-docker.properties`** | Docker 실행 시 DB 경로·JPA 설정. |

---

## 5. Dockerfile이 하는 일 (읽는 순서)

1. **빌드 스테이지 (`builder`)**  
   - `eclipse-temurin:17-jdk-alpine` 이미지로 Gradle Wrapper를 실행합니다.  
   - `./gradlew bootJar`로 실행 가능한 JAR를 만듭니다.  
   - Gradle은 `*-plain.jar`(라이브러리 미포함)와 **실행용 fat JAR**를 둘 다 만들 수 있어, **`plain`이 아닌 JAR 하나**만 골라 `/app/app.jar`로 복사합니다.

2. **실행 스테이지**  
   - `eclipse-temurin:17-jre-alpine`만 사용해 이미지를 가볍게 유지합니다.  
   - `EXPOSE 8080`은 “이 포트를 쓴다”는 **문서/힌트**이며, 실제로 밖으로 열 포트는 `docker-compose.yml`의 `ports`에서 정합니다.

3. **엔트리포인트**  
   - `java -jar app.jar`로 Spring Boot를 기동합니다.  
   - 필요하면 `JAVA_OPTS` 환경 변수로 메모리 등을 줄 수 있게 해 두었습니다.

---

## 6. docker-compose가 하는 일

- **`build: .`**  
  현재 폴더의 `Dockerfile`로 이미지를 빌드합니다.  
- **`ports: "8080:8080"`**  
  PC의 `http://localhost:8080` → 컨테이너의 8080으로 연결합니다.  
- **`volumes: crud2-h2-data:/data`**  
  H2 파일 DB가 저장되는 `/data`를 Docker 볼륨에 묶어, **컨테이너를 삭제해도 볼륨을 남기면 데이터가 남을 수 있습니다** (볼륨까지 지우면 초기화됩니다).

---

## 7. 실행 방법 (차근차근)

### 7-1. 터미널에서 프로젝트 루트로 이동

```powershell
cd d:\spring1\crud2
```

(WSL이면 경로만 맞춥니다.)

### 7-2. 이미지 빌드 + 컨테이너 기동

```powershell
docker compose up --build
```

- **처음**에는 이미지 빌드에 시간이 걸립니다.  
- 로그에 Spring Boot 배너와 `Started Crud2Application` 비슷한 메시지가 보이면 성공입니다.

### 7-3. 브라우저에서 확인

- 목록: [http://localhost:8080/list](http://localhost:8080/list)

로컬에서 `bootRun` 할 때와 **같은 URL**입니다.

### 7-4. 백그라운드 실행 (선택)

```powershell
docker compose up -d --build
```

종료:

```powershell
docker compose down
```

데이터 볼륨까지 지우고 완전히 초기화하려면:

```powershell
docker compose down -v
```

`-v`는 **이 compose 파일이 만든 볼륨**을 삭제하므로, H2 DB 파일도 함께 사라집니다.

---

## 8. Docker만 쓰고 compose 없이 실행하고 싶다면

프로젝트 루트에서:

```powershell
docker build -t crud2:latest .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker crud2:latest
```

이 경우 **볼륨을 안 붙이면** 컨테이너를 지울 때 H2 파일도 같이 사라집니다. 데이터를 남기려면 `-v`로 호스트 디렉터리나 이름 있는 볼륨을 `/data`에 마운트해야 합니다.

---

## 9. 자주 겪는 문제

### 9-1. `docker compose` 명령을 찾을 수 없음

- Docker Desktop이 실행 중인지 확인합니다.  
- 구버전은 `docker-compose`(하이픈)일 수 있습니다.  
  ```powershell
  docker-compose up --build
  ```

### 9-2. 포트 8080이 이미 사용 중

- 다른 프로그램(또는 로컬에서 돌린 Spring Boot)이 8080을 쓰고 있을 수 있습니다.  
- `docker-compose.yml`의 포트를 예를 들어 `"8081:8080"`으로 바꾸면 브라우저는 `http://localhost:8081/list` 로 접속합니다.

### 9-3. 빌드가 느리거나 실패한다

- `.dockerignore`로 `build/`, `.gradle/`을 제외해 두었습니다.  
- 회사망에서는 Gradle이 의존성을 받지 못할 수 있습니다. VPN/프록시 설정을 확인합니다.

### 9-4. Windows에서 `gradlew` 줄바꿈(CRLF) 때문에 Linux 빌드 실패

- Docker 빌드는 Linux 환경에서 `gradlew`를 실행합니다.  
- 오류가 나면 Git에서 `gradlew`를 **LF**로 체크아웃하거나, IDE에서 해당 파일만 LF로 저장해 보세요.

---

## 10. 운영·학습용으로 MySQL을 쓰고 싶다면

지금 `crud2`는 **H2** 기준입니다. 실제 서비스에서는 MySQL 등을 쓰는 경우가 많습니다. 그때는 대략 다음 순서입니다.

1. `build.gradle`에 `mysql-connector-j`를 `runtimeOnly`로 추가 (주석 해제).  
2. `application-docker.properties`(또는 별도 프로파일)에 MySQL URL·계정을 설정.  
3. `docker-compose.yml`에 **`mysql` 서비스**를 추가하고, 앱이 기동될 때 DB가 뜨도록 `depends_on` 등을 맞춥니다.

이 부분은 배포 환경마다 달라서, 필요하면 그때 별도 문서로 정리하는 것이 좋습니다.

---

## 11. 요약

1. **Docker Desktop** 설치 후 실행.  
2. **`crud2` 루트**에서 `docker compose up --build`.  
3. 브라우저에서 **`http://localhost:8080/list`**.  
4. Docker 실행 시에는 **`SPRING_PROFILES_ACTIVE=docker`** 로 H2가 **`/data`** 아래 파일 DB를 사용하고, **compose 볼륨**으로 데이터를 유지할 수 있습니다.

이 순서대로 따라가면 **crud2를 도커로 배포**하는 흐름을 처음부터 재현할 수 있습니다.
