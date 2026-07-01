# crud2 배포 전 과정 정리 (로컬 Docker → Docker Hub → EC2 + RDS)

이 문서는 **crud2** 프로젝트를 로컬에서 Docker로 실행하고, **Docker Hub**에 이미지를 올린 뒤 **AWS EC2**에서 **Amazon RDS MySQL**에 연결해 서비스까지 띄우는 과정에서 겪은 설정·오류·해결을 한곳에 정리한 것입니다.

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 경로 | `d:\spring1\crud2` |
| 스택 | Spring Boot 3.3.x, Java 17, Thymeleaf, JPA, Lombok |
| 로컬 DB | H2 (`application.properties`) |
| 운영 DB | MySQL on RDS (`application-prod.properties`, 프로파일 `prod`) |
| Docker 이미지 | `Dockerfile`로 fat JAR 빌드, 예: `bhher30/crud2:latest` (Docker Hub) |

관련 파일:

- `build.gradle` — `runtimeOnly` 로 H2 + MySQL 드라이버 둘 다 포함 (로컬 H2 / 운영 MySQL)
- `src/main/resources/application.properties` — 기본 H2
- `src/main/resources/application-prod.properties` — `SPRING_DATASOURCE_*` 환경 변수로 MySQL 연결
- `Dockerfile` — 멀티 스테이지 빌드 후 JRE 이미지에서 `java -jar app.jar`
- `docker-compose.yml` — 로컬 H2 파일 DB용 (선택)
- 기타 문서: `docs/docker-deployment.md`, `docs/aws-docker-mysql-deploy.md`, `docs/ec2-docker-rds.md`

---

## 2. 로컬에서 Docker로 실행할 때

### 2-1. 접속 URL

- .\gradlew.bat bootRun

- 루트 `/` 는 매핑이 없을 수 있음 → **`http://localhost:8080/list`** 로 접속.

### 2-2. Docker Desktop

- 오류: `failed to connect to the docker API ... dockerDesktopLinuxEngine`  
- **해결:** Docker Desktop 실행 후 Engine이 뜰 때까지 대기.

### 2-3. 컨테이너 이름 충돌

- 오류: `Conflict. The container name "/crud2-app" is already in use`  
- **해결:** `docker rm -f crud2-app` 후 다시 `docker run`, 또는 `docker start crud2-app`.

### 2-4. H2 드라이버 없음 (이미지 재빌드 전)

- `build.gradle`에서 H2를 주석 처리한 상태로 `application.properties`만 H2면 JAR에 드라이버가 없음.  
- 오류: `Cannot load driver class: org.h2.Driver`  
- **해결:** `runtimeOnly 'com.h2database:h2'` 복구 후 **`docker build --no-cache -t crud2:latest .`** 로 다시 빌드.

### 2-5. MySQL(RDS) 운영용

- `application-prod.properties` + 환경 변수 `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL` 등.

---

## 3. Windows에서 SSH로 EC2 접속

### 3-1. 사용자 이름

- **Amazon Linux** → `ec2-user@퍼블릭IP` (Ubuntu가 아님)  
- `ec2-user@ 52.x.x.x` 처럼 **`@` 뒤 공백 없이** 입력.

### 3-2. `.pem` 권한 (OpenSSH)

- 오류: `UNPROTECTED PRIVATE KEY FILE`, `bad permissions`  
- **PowerShell:**

```powershell
icacls "D:\spring1\crud2\crud21-key.pem" /inheritance:r
icacls "D:\spring1\crud2\crud21-key.pem" /grant:r "$($env:USERNAME):R"
```

- `(R)` 이 아니라 **`:R`** (잘못된 `icacls` 매개변수 주의).

### 3-3. 키 페어 일치

- 인스턴스 생성 시 선택한 **키 페어**와 **다운로드한 `.pem`** 이 짝이어야 함.  
- 오류: `Permission denied (publickey)` → 다른 키를 쓰는 경우가 많음.

### 3-4. 파일 이름

- 같은 키 내용을 복사해 이름만 바꿔서 사용해도 됨. 파일마다 `icacls` 적용.

---

## 4. Docker Hub에 올리고 EC2에서 받기

1. 로컬: `docker build -t crud2:latest .`  
2. `docker tag crud2:latest bhher30/crud2:latest`  
3. `docker login` 후 `docker push bhher30/crud2:latest`  
4. EC2: `docker pull bhher30/crud2:latest`

오류: `No such image: crud2:latest` → 로컬에 이미지가 없을 때. 먼저 `docker build` 또는 `docker images`로 확인.

---

## 5. EC2에서 `docker run` (Linux bash)

- **PowerShell 백틱 `` ` `` 는 Linux bash에서 쓰면 안 됨.** 줄 끝은 **`\`** 또는 **한 줄**로 작성.

예시 (값은 본인 환경에 맞게 수정):

```bash
docker rm -f crud2-app 2>/dev/null

docker run -d -p 8080:8080 --name crud2-app \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://crud2-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/crud2?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME="admin" \
  -e SPRING_DATASOURCE_PASSWORD='RDS비밀번호' \
  bhher30/crud2:latest
```

- 브라우저: **`http://EC2퍼블릭IP:8080/list`**  
- EC2 **보안 그룹** 인바운드 **TCP 8080** 필요.

---

## 6. RDS 보안 그룹 (MySQL 3306)

- 앱이 **EC2**에서 RDS로 접속하려면 RDS 보안 그룹 **인바운드**에 **TCP 3306** 허용 필요.  
- **권장:** 소스 = **EC2 인스턴스의 보안 그룹**  
- **테스트:** 소스 `0.0.0.0/0` + 유형 **MYSQL/Aurora**, 포트 **3306** (운영 전에는 좁히기)

오류: `Communications link failure`, `SocketTimeoutException: Connect timed out`  
→ **네트워크/방화벽** (대부분 RDS SG가 EC2에서 오는 트래픽을 허용하지 않음).  
위 규칙 수정 후 컨테이너 재실행.

---

## 7. 문제 해결 순서 요약

| 증상 | 점검 |
|------|------|
| 로컬 404 @ `/` | `/list` 사용 |
| Docker API 연결 실패 | Docker Desktop 실행 |
| 컨테이너 이름 충돌 | `docker rm -f crud2-app` |
| H2 드라이버 로드 실패 | `build.gradle`에 H2 + 이미지 재빌드 |
| SSH publickey 거절 | 키 파일·페어 일치, `ec2-user`, pem 권한 |
| `docker run` 파싱 오류 (EC2) | bash에서는 `` ` `` 대신 `\` 또는 한 줄 |
| RDS 연결 타임아웃 | RDS SG 3306 + EC2와 같은 VPC 등 |
| 브라우저에서 EC2 접속 안 됨 | EC2 SG에 8080(또는 80) 인바운드 |

---

## 8. 보안 권장 (운영 전)

- RDS 비밀번호를 채팅·스크립트에 장기 보관하지 않기; 가능하면 변경 후 **Secrets Manager** 등 사용.  
- RDS 인바운드 `0.0.0.0/0` 은 테스트 후 **EC2 보안 그룹만** 허용으로 변경.  
- `.pem` 은 Git에 커밋하지 않기 (`.gitignore`).

---

## 9. 관련 문서 (같은 프로젝트)

- `docs/docker-deployment.md` — 로컬 Docker / Compose  
- `docs/aws-docker-mysql-deploy.md` — AWS + MySQL 개요  
- `docs/ec2-docker-rds.md` — EC2 + Docker + RDS 순서  
- `docker/docker-basics.md` (저장소 루트 `spring1/docker`) — Docker 개념

---

이 문서는 대화에서 다룬 **crud2 Docker·AWS 배포·트러블슈팅**을 기준으로 작성되었습니다. 실제 엔드포인트·계정·IP는 배포 시 본인 콘솔 값으로 바꿔 사용하세요.
