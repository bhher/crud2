# crud2 — 집에서 하기: RDS 만들기 ~ 접속 설정

Amazon RDS MySQL을 **처음부터 만들고**, crud2(Spring Boot + Docker)와 **연결하는 방법**만 정리한 문서입니다.

> EC2 Docker 배포는 [`집에서하기-EC2-Docker-RDS-1to9.md`](./집에서하기-EC2-Docker-RDS-1to9.md) 를 이어서 보세요.

---

## 전체 그림

```
[브라우저] ──8080──▶ [Ubuntu EC2 + Docker crud2]
                           │
                           └── JDBC 3306 ──▶ [RDS MySQL crud-db / crud2_db]
```

- **RDS** = AWS가 관리하는 MySQL (백업·패치)
- **EC2 Docker** = crud2 앱 실행
- **로컬 PC**는 RDS에 **직접 붙지 않음** (퍼블릭 액세스 아니요 + EC2만 3306 허용)

---

## 준비물

| 항목 | 내용 |
|------|------|
| AWS 계정 | 콘솔 로그인 |
| 리전 | **아시아 태평양(서울) ap-northeast-2** (EC2·RDS 같은 리전) |
| crud2 프로젝트 | `d:\spring1\crud2` |
| 메모장 | 마스터 비밀번호·엔드포인트 (Git/채팅에 올리지 않기) |

---

## 1단계 — RDS 데이터베이스 생성 (콘솔)

1. AWS 콘솔 → **RDS** → **데이터베이스** → **데이터베이스 생성**
2. **표준 생성** 선택

### 1-1. 엔진 및 템플릿

| 항목 | 권장 값 |
|------|---------|
| 엔진 | **MySQL** |
| 버전 | **MySQL 8.x** (기본 최신) |
| 템플릿 | **프리 티어** (학습용) |

### 1-2. 설정

| 항목 | 권장 값 | 설명 |
|------|---------|------|
| **DB 식별자** | `crud-db` | RDS 인스턴스 이름 (콘솔에 보이는 이름) |
| **마스터 사용자 이름** | `admin` | Spring에서 DB 접속 ID |
| **마스터 암호** | (직접 설정) | **8자 이상**, 반드시 메모 |
| **암호 확인** | 동일 입력 | |

> ⚠️ 비밀번호는 **한 번만** 설정·기억. 잊으면 RDS 수정에서 재설정 필요.

### 1-3. 인스턴스 구성

| 항목 | 권장 값 |
|------|---------|
| DB 인스턴스 클래스 | **db.t3.micro** 또는 **db.t4g.micro** (프리 티어) |
| 스토리지 | 기본 20GB (프리 티어) |

### 1-4. 연결 (중요)

| 항목 | 권장 값 | 이유 |
|------|---------|------|
| **VPC** | **기본 VPC** (또는 EC2와 **같은 VPC**) | EC2↔RDS 통신 |
| **퍼블릭 액세스** | **아니요** | EC2 Docker 배포 시 보안 |
| VPC 보안 그룹 | **새로 생성** 또는 **default** | 3단계에서 3306 규칙 추가 |
| **가용 영역** | 기본값 OK | |
| **데이터베이스 포트** | **3306** | MySQL 기본 |

### 1-5. 추가 구성

| 항목 | 권장 값 |
|------|---------|
| **초기 데이터베이스 이름** | **`crud2_db`** | JDBC URL `/crud2_db` 와 동일 |
| 백업 | 기본값 (프리 티어) |
| 암호화 | 기본값 |

### 1-6. 생성

- **데이터베이스 생성** 클릭
- 상태가 **사용 가능** 될 때까지 **5~10분** 대기

---

## 2단계 — RDS 연결 정보 확인

**RDS** → **데이터베이스** → `crud-db` 클릭 → **연결 및 보안** 탭

| 항목 | 어디서 | 예시 |
|------|--------|------|
| **엔드포인트** | 「엔드포인트」 라디오 선택 후 복사 | `crud-db.xxxxx.ap-northeast-2.rds.amazonaws.com` |
| **포트** | 3306 | |
| **VPC** | vpc-xxxxx | EC2와 **같은지** 나중에 확인 |
| **퍼블릭 액세스** | 아니요 | |
| **VPC 보안 그룹** | sg-xxxxx 링크 | 3단계에서 수정 |

**구성** 탭에서:

- **DB 이름**: `crud2_db` (초기 DB 이름 넣었으면 표시)
- **마스터 사용자명**: `admin` 등

메모 예:

```text
엔드포인트: crud-db.xxxxx.ap-northeast-2.rds.amazonaws.com
포트: 3306
DB: crud2_db
사용자: admin
비밀번호: (본인이 정한 값)
```

---

## 3단계 — RDS 보안 그룹 (3306)

EC2에서 Docker crud2가 RDS에 붙으려면 **3306**을 **EC2 보안 그룹**에만 열어야 합니다.

> EC2를 **아직** 안 만들었다면: EC2 만든 **뒤** 이 단계를 하거나, 일단 RDS만 만들고 EC2 SG ID 나오면 추가.

1. RDS → `crud-db` → **연결 및 보안** → **VPC 보안 그룹** (예: `default` 또는 `sg-0b6db47f9457e9b57`) 클릭
2. **인바운드 규칙** → **인바운드 규칙 편집**
3. **규칙 추가**:

| 유형 | 포트 | 원본 유형 | 원본 |
|------|------|-----------|------|
| **MYSQL/Aurora** | **3306** | **보안 그룹** | **EC2 보안 그룹** (`launch-wizard-1` 의 `sg-xxxx`) |

- **원본**에는 `8080` 넣지 않음 (8080은 EC2 웹용)
- **0.0.0.0/0** 넣지 않음 (인터넷 전체 DB 개방 X)
- EC2 SG ID 확인: **EC2** → 인스턴스 → **보안** 탭 → 보안 그룹 ID

4. **규칙 저장**

---

## 4단계 — crud2 Spring 설정 (`application-prod.properties`)

파일: `src/main/resources/application-prod.properties`

### 4-1. JDBC URL

2단계에서 복사한 **엔드포인트**로 URL 작성:

```properties
spring.datasource.url=jdbc:mysql://crud-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/crud2_db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
```

- `xxxxx` → 본인 RDS **엔드포인트** 전체
- `/crud2_db` → 1단계 **초기 데이터베이스 이름**과 동일

### 4-2. 계정 (권장: 환경 변수)

**Git에 비밀번호 넣지 않으려면:**

```properties
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

**집에서만 테스트** (커밋하지 않을 때만):

```properties
spring.datasource.username=admin
spring.datasource.password=본인RDS비밀번호
```

### 4-3. JPA / MySQL

```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=false
spring.thymeleaf.cache=true
```

| ddl-auto | 설명 |
|----------|------|
| `update` | 첫 배포 시 **테이블 자동 생성** (학습용 편함) |
| `validate` | 테이블이 **이미 있어야** 함 (운영에 가까움) |

첫 배포면 **`update`** 권장.

### 4-4. 프로파일

로컬 IDE / Docker 실행 시:

```text
SPRING_PROFILES_ACTIVE=prod
```

---

## 5단계 — `build.gradle` MySQL 드라이버 확인

`d:\spring1\crud2\build.gradle` 에 다음이 있는지 확인:

```gradle
runtimeOnly 'com.mysql:mysql-connector-j'
runtimeOnly 'com.h2database:h2'   // 로컬 H2용
```

없으면 추가 후 저장.

---

## 6단계 — (선택) RDS DB / 테이블 확인

초기 DB 이름 `crud2_db` 를 **만들 때 넣지 않았다면** MySQL 클라이언트로 생성:

```sql
CREATE DATABASE crud2_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> EC2 Docker 배포 후 `ddl-auto=update` 이면 crud2 기동 시 **테이블은 JPA가 생성**합니다.

---

## 7단계 — EC2 Docker에서 RDS 연결 (실행)

EC2 SSH 접속 후 (Ubuntu):

```bash
docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD='RDS비밀번호' \
  crud2:latest
```

- URL이 **prod 파일에 있으면** `-e SPRING_DATASOURCE_URL` 불필요
- URL·계정을 **전부 환경 변수**로만 쓰려면:

```bash
docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://엔드포인트:3306/crud2_db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul' \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD='RDS비밀번호' \
  crud2:latest
```

로그 확인:

```bash
docker logs crud2-app
```

DB 연결 성공 시 `Started Crud2Application` 근처에 **HikariPool started** 등이 보입니다.

---

## 8단계 — 연결 테스트

| 방법 | URL / 명령 |
|------|------------|
| 브라우저 | `http://EC2퍼블릭IP:8080/list` |
| EC2 로그 | `docker logs crud2-app` |

성공: 목록 페이지 표시 + 로그에 DB 연결 오류 없음.

---

## 9단계 — 체크리스트

### RDS 생성

- [ ] 리전 **서울(ap-northeast-2)**
- [ ] DB 식별자 `crud-db` (또는 본인 이름)
- [ ] 초기 DB **`crud2_db`**
- [ ] 마스터 사용자·비밀번호 메모
- [ ] 상태 **사용 가능**
- [ ] **엔드포인트** 복사

### 보안

- [ ] 퍼블릭 액세스 **아니요** (EC2 Docker 배포)
- [ ] RDS SG: **3306 ← EC2 SG** (`sg-...`)
- [ ] EC2와 RDS **같은 VPC**

### crud2 설정

- [ ] `application-prod.properties` JDBC URL `/crud2_db`
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `mysql-connector-j` in `build.gradle`
- [ ] `docker build` 후 EC2에서 `docker run`

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| `Communications link failure` | RDS SG에 EC2 SG 없음 | 3306 인바운드 EC2 SG 추가 |
| | VPC 다름 | EC2·RDS 같은 VPC |
| | RDS 아직 생성 중 | **사용 가능** 될 때까지 대기 |
| `Access denied for user` | 사용자/비밀번호 오류 | RDS 마스터 계정 재확인 |
| `Unknown database 'crud2_db'` | DB 미생성 | 초기 DB 이름 또는 `CREATE DATABASE` |
| `validate` + 테이블 없음 | ddl-auto | `update`로 변경 또는 SQL로 테이블 생성 |
| 로컬 PC에서 RDS 접속 안 됨 | 퍼블릭 아니요 | **정상** — EC2 Docker로만 접속 |

---

## (참고) 로컬 PC에서 RDS 직접 붙이고 싶을 때

EC2 없이 **로컬 Spring Boot → RDS** 테스트:

1. RDS **수정** → 퍼블릭 액세스 **예**
2. RDS SG → 3306 ← **내 PC 공인 IP**/32
3. 로컬 `bootRun` + `prod` 프로파일

배포는 **퍼블릭 아니요 + EC2만 3306** 이 안전합니다.

---

## JDBC URL 한 줄 요약

```text
jdbc:mysql://[RDS엔드포인트]:3306/crud2_db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
```

예 (본인 값으로 교체):

```text
jdbc:mysql://crud-db.cnau2o8em3i3.ap-northeast-2.rds.amazonaws.com:3306/crud2_db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
```

---

## 다음 문서

| 순서 | 문서 |
|------|------|
| RDS (이 문서) | **1~9단계 RDS 만들기·설정** |
| EC2 Docker (요약) | [`집에서하기-EC2-Docker-RDS-1to9.md`](./집에서하기-EC2-Docker-RDS-1to9.md) |
| EC2 Docker (실습 2) | [`집에서하기-2-EC2-Docker-RDS.md`](./집에서하기-2-EC2-Docker-RDS.md) |
| 상세 | [`ec2-docker-rds.md`](./ec2-docker-rds.md), [`aws-docker-mysql-deploy.md`](./aws-docker-mysql-deploy.md) |

---

*문서 위치: `crud2/docs/집에서하기-RDS-만들기-설정.md`*
