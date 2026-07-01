# Spring Boot + Docker + AWS(EC2 + RDS) 배포 가이드

Spring Boot 애플리케이션을 **Docker 이미지**로 만들고 **AWS EC2**에서 실행하며 **Amazon RDS MySQL**에 연결하는 흐름을 정리한 문서입니다.

---

## 전체 구조

```text
사용자 → EC2 (Docker 안의 Spring Boot) → RDS (MySQL)
```

- **EC2**: 웹 애플리케이션이 도는 서버 (Docker 컨테이너).
- **RDS**: 관리형 MySQL (EC2 안에 DB를 직접 설치하지 않음).
- **Docker**: 빌드한 JAR + JRE를 이미지로 묶어 동일 환경에서 실행.

---

## 1. Docker 개념

- **이미지**: 실행에 필요한 파일·설정을 묶은 패키지.
- **컨테이너**: 이미지를 실행한 인스턴스.
- 같은 이미지면 로컬·EC2에서 **동일하게** 동작하기 쉽다.

자세한 용어는 `docker/docker-basics.md`(저장소 `spring1/docker`) 참고.

---

## 2. Spring Boot → Docker 이미지

### 2-1. Dockerfile 예시 (단순 형태)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> 실제 `crud2` 프로젝트는 **Gradle로 JAR를 만드는 멀티 스테이지** `Dockerfile`을 사용할 수 있다. `bootJar` 결과물 이름에 맞춰 `COPY` 경로를 조정한다.

### 2-2. 빌드

```bash
./gradlew bootJar
docker build -t crud2:latest .
```

---

## 3. AWS RDS(MySQL) 생성

### 3-1. 개념

- **RDS** = AWS가 운영·백업·패치를 담당하는 DB 서버.
- EC2에 MySQL을 직접 깔지 않고 **별도 RDS**를 두는 구성이 일반적이다.

### 3-2. 설정 예

| 항목 | 예시 |
|------|------|
| 엔진 | MySQL |
| 인스턴스 클래스 | db.t3.micro 등 |
| 마스터 사용자명 | admin 등 |
| 비밀번호 | 콘솔에서 설정 (안전하게 보관) |
| 퍼블릭 액세스 | 학습용으로 YES 가능, 운영은 VPC 설계에 따라 다름 |

### 3-3. 보안 그룹(초기)

- 인바운드 **TCP 3306**
- 소스: **내 PC IP** (로컬에서 `mysql` 클라이언트로 테스트할 때)

### 3-4. 데이터베이스 생성

RDS에 접속 후 스키마(데이터베이스) 생성 예:

```sql
CREATE DATABASE crud2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

(JDBC URL의 경로 `/crud2`와 이름을 맞출 것.)

---

## 4. MySQL 접속 테스트 (로컬)

```bash
mysql -h <RDS엔드포인트> -u admin -p
```

---

## 5. Spring Boot 운영 설정 (`application-prod.properties`)

환경 변수로 주입하는 방식 예:

```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

실행 시:

- `SPRING_PROFILES_ACTIVE=prod`

---

## 6. Docker로 로컬에서 RDS 연결 테스트

**Linux / Mac / Git Bash** (줄 끝 `\`):

```bash
docker run -d -p 8080:8080 --name crud2-app \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://<RDS주소>:3306/crud2?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD='비밀번호' \
  crud2:latest
```

브라우저: `http://localhost:8080/list` (프로젝트에 루트 매핑이 없으면 `/list` 사용.)

---

## 7. EC2 생성

| 항목 | 예시 |
|------|------|
| AMI | Ubuntu **또는** Amazon Linux 2023 |
| 인스턴스 타입 | t2.micro / t3.micro 등 |
| 키 페어 | `.pem` 다운로드 |

### 보안 그룹(예)

| 유형 | 포트 | 소스(예) |
|------|------|----------|
| SSH | 22 | 내 IP |
| HTTP | 80 | 0.0.0.0/0 (또는 제한) |
| Custom TCP | 8080 | 0.0.0.0/0 (앱을 8080에 올릴 때) |

---

## 8. EC2 접속 (SSH)

**Ubuntu AMI:**

```bash
ssh -i "키파일.pem" ubuntu@<퍼블릭IP>
```

**Amazon Linux AMI:**

```bash
ssh -i "키파일.pem" ec2-user@<퍼블릭IP>
```

- `ubuntu@` 또는 `ec2-user@` **바로 뒤에 공백 없이** IP를 붙인다.

### Windows `.pem` 권한 (OpenSSH)

```powershell
icacls "D:\경로\키.pem" /inheritance:r
icacls "D:\경로\키.pem" /grant:r "$($env:USERNAME):R"
```

---

## 9. EC2에 Docker 설치

**Ubuntu:**

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl start docker
sudo usermod -aG docker ubuntu
```

**Amazon Linux 2023:**

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

재로그인 후 `docker ps` 로 확인.

---

## 10. Docker Hub에 이미지 올리기

```bash
docker login
docker tag crud2:latest <DockerHub아이디>/crud2:latest
docker push <DockerHub아이디>/crud2:latest
```

---

## 11. EC2에서 이미지 받아 실행

```bash
docker pull <DockerHub아이디>/crud2:latest
```

```bash
docker rm -f crud2-app 2>/dev/null

docker run -d -p 80:8080 --name crud2-app \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://<RDS주소>:3306/crud2?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD='비밀번호' \
  <DockerHub아이디>/crud2:latest
```

- `-p 80:8080` 이면 브라우저는 **`http://<EC2퍼블릭IP>/list`** (포트 생략).
- `-p 8080:8080` 이면 **`http://<EC2퍼블릭IP>:8080/list`**.

> EC2 터미널은 **Linux bash**이다. **PowerShell 백틱 `` ` ``** 로 줄 이으면 안 되고, **`\`** 또는 **한 줄**로 작성한다.

---

## 12. RDS 보안 그룹 수정 (중요)

| 단계 | 설명 |
|------|------|
| 초기 | “내 PC IP만” 3306 허용 → **EC2에서 돌아가는 앱**은 RDS에 붙지 못할 수 있음 |
| 수정 | 인바운드 **MySQL/Aurora, TCP 3306**, 소스 = **EC2 인스턴스의 보안 그룹** (권장) |
| 테스트용 | 소스 `0.0.0.0/0` (가능하지만 운영에서는 축소) |

수정 후 앱 컨테이너를 다시 띄운다 (`docker rm` → `docker run`).

---

## 13. 접속 확인

- `http://<EC2퍼블릭IP>/list` 또는 `http://<EC2퍼블릭IP>:8080/list` (포트 매핑에 맞게)
- EC2 보안 그룹에 **80 또는 8080** 인바운드가 열려 있어야 한다.

---

## 14. 비용·중지 시 참고

| 리소스 | 중지/삭제 시 |
|--------|----------------|
| **EC2** | Stop 시 컴퓨트 요금 대부분 중단. **EBS**는 남아 있으면 스토리지 요금 가능. |
| **EC2 Terminate** | 인스턴스 삭제. EBS를 삭제 옵션과 함께 정리해야 스토리지 과금 정리에 유리. |
| **RDS** | Stop 시에도 **스토리지 등** 과금이 남을 수 있음. 완전 해제는 **삭제(Snapshot 여부 선택)** 등을 검토. 자동 시작 정책은 리전·문서 확인. |

과금은 **AWS 요금 페이지·청구 콘솔** 기준이 정확하다.

---

## 15. 주의사항

- `.pem` 권한: Windows에서는 `icacls`로 본인만 읽기 권한.
- EC2 **Stop/Start** 후 **퍼블릭 IP**가 바뀔 수 있음 (Elastic IP 미사용 시).
- DB 비밀번호·JDBC URL은 **환경 변수·Secrets Manager** 등으로 관리하는 것이 안전하다.

---

## 16. 한 줄 정리

**Spring Boot를 Docker 이미지로 만들고, EC2에서 실행하며 RDS MySQL에 붙이는 구성**은 실무에서 자주 쓰는 배포 패턴의 하나이다.

---

## 관련 문서 (이 저장소)

- `docs/deployment-full-journey.md` — 이번 프로젝트에서 겪은 상세 트러블슈팅
- `docs/docker-deployment.md` — 로컬 Docker
- `docs/aws-docker-mysql-deploy.md` — AWS·MySQL
- `docs/ec2-docker-rds.md` — EC2 + Docker + RDS 순서
