# crud2 — 집에서 하기 2 (EC2 + Docker + RDS 실습본)

Ubuntu 또는 Amazon Linux EC2에 Docker로 **crud2 앱**을 올리고, **AWS RDS MySQL**에 연결하는 **실습·복습용** 가이드입니다.

> **1편 (RDS 만들기):** [`집에서하기-RDS-만들기-설정.md`](./집에서하기-RDS-만들기-설정.md)  
> **요약본:** [`집에서하기-EC2-Docker-RDS-1to9.md`](./집에서하기-EC2-Docker-RDS-1to9.md)

---

## 전체 그림

```
[내 PC 브라우저] ──8080──▶ [EC2 + Docker crud2 앱] ──3306──▶ [RDS MySQL]
```

- **`docker run`** = **앱(Spring Boot)** 컨테이너만 실행
- **RDS** = AWS에서 **미리 만든** DB (이 명령으로 DB가 뜨지 않음)

---

## 사전 준비 (메모해 둘 것)

| 항목 | 예시 / 확인 위치 |
|------|------------------|
| RDS 엔드포인트 | `crud2-db.xxxxx.ap-northeast-2.rds.amazonaws.com` (콘솔 복사) |
| RDS DB 이름 | `crud2_db` |
| RDS 사용자 / 비밀번호 | `admin` / (본인 비밀번호) |
| EC2 퍼블릭 IP | 예: `3.36.101.101` |
| EC2 VPC | RDS와 **동일** (예: `vpc-0d5085e7e278dfbe9`) |
| EC2 보안 그룹 ID | 예: `launch-wizard-1` → `sg-xxxx` |
| SSH 키 `.pem` | EC2 **키 페어 이름**과 동일한 파일 |

> AWS에 RDS 식별자를 `crud2-db`로 만들었다면 엔드포인트 호스트도 `crud2-db.xxx` 입니다. 문서의 `crud`/`crud2`는 **오타 `curd`를 고친 표기**이며, **실제 엔드포인트는 콘솔 값**을 씁니다.

---

## 1단계 — AWS 준비 확인

### RDS

| 항목 | 확인 |
|------|------|
| 상태 | **사용 가능** |
| 엔드포인트 | 연결 및 보안 → **엔드포인트** 라디오 선택 후 복사 |
| DB 이름 | `crud2_db` |
| 퍼블릭 액세스 | **아니요** (EC2만 접속) |
| 마스터 계정 | docker run `-e` 또는 prod 파일에 사용 |

### 로컬 PC

| 파일 | 경로 예 |
|------|---------|
| 프로젝트 | `d:\spring1\crud2` 또는 `E:\spring1\crud2` |
| SSH 키 | `crud2-key.pem`, `crud22-key.pem` 등 (EC2 키 페어와 짝) |
| prod 설정 | `src/main/resources/application-prod.properties` |

---

## 2단계 — EC2 만들기

| 항목 | 값 |
|------|-----|
| AMI | **Ubuntu 22.04** (또는 Amazon Linux 2023) |
| 유형 | `t3.micro` |
| 키 페어 | `.pem` 다운로드·보관 |
| VPC | **RDS와 동일** |
| 퍼블릭 IP | 활성화 |

**보안 그룹 인바운드**

| 유형 | 포트 | 원본 |
|------|------|------|
| SSH | 22 | 내 IP |
| 사용자 지정 TCP | **8080** | 내 IP (또는 학습용 `0.0.0.0/0`) |

- **포트 8080** → **포트 범위** 칸에 입력
- **원본**에는 IP만 (8080 숫자 넣지 않음)

시작 후 **퍼블릭 IP**, **보안 그룹 ID** (`sg-...`) 메모.

---

## 3단계 — RDS 보안 그룹 (3306)

1. RDS → DB 인스턴스 → **연결 및 보안** → VPC 보안 그룹
2. **인바운드 규칙 편집** → 추가:

| 유형 | 포트 | 원본 |
|------|------|------|
| MYSQL/Aurora | 3306 | **EC2 보안 그룹** (`sg-xxxx`) |

3. **규칙 저장**

---

## 4단계 — pem 권한 + SSH

### AMI별 SSH 사용자

| AMI | 사용자 |
|-----|--------|
| **Ubuntu** | `ubuntu` |
| **Amazon Linux** | `ec2-user` |

### Windows — pem 권한

```powershell
icacls "d:\spring1\crud2\crud2-key.pem" /inheritance:r
icacls "d:\spring1\crud2\crud2-key.pem" /grant:r "$($env:USERNAME):(F)"
icacls "d:\spring1\crud2\crud2-key.pem" /remove "Authenticated Users"
icacls "d:\spring1\crud2\crud2-key.pem" /remove "Users"
```

### SSH 예시

**Ubuntu:**

```powershell
ssh -i "d:\spring1\crud2\crud2-key.pem" ubuntu@EC2퍼블릭IP
```

**Amazon Linux:**

```powershell
ssh -i "C:\Users\본인계정\Downloads\crud22-key.pem" ec2-user@EC2퍼블릭IP
```


ssh -i "C:\Users\TJ\Downloads\crud22-key.pem" ec2-user@15.165.42.239 

- `Permission denied (publickey)` → 키 페어 이름 ≠ pem 파일
- `type -1` (ssh -v) → icacls 다시 또는 WSL `chmod 400`

---

## 5단계 — EC2에 Docker 설치

### Ubuntu

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
```

### Amazon Linux 2023

```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

**로그아웃 후 SSH 재접속** → `docker --version` 확인.

---

## 6단계 — 로컬 PC에서 이미지 빌드

```powershell
cd d:\spring1\crud2
docker build -t crud2:latest .
docker save crud2:latest -o crud2-image.tar
```

`application-prod.properties` 수정 후에는 **반드시 다시 build**.

---

## 7단계 — 이미지 EC2로 전송 (scp)

**방법 1 — 프로젝트 폴더에서**

```powershell
cd d:\spring1\crud2
scp -i "d:\spring1\crud2\crud2-key.pem" crud2-image.tar ubuntu@EC2퍼블릭IP:~/
```

**방법 2 — 전체 경로**

```powershell
scp -i "C:\Users\본인\Downloads\crud22-key.pem" E:\spring1\crud2\crud2-image.tar ec2-user@EC2퍼블릭IP:~/
```

- Ubuntu → `ubuntu@IP`
- Amazon Linux → `ec2-user@IP`
- 키·사용자·IP는 **본인 환경**에 맞게 변경

---

## 8단계 — EC2에서 로드 + docker run (RDS 연결)

EC2 SSH 접속 후:

```bash
docker load -i crud2-image.tar
docker images
```

### 방법 A — 환경 변수로 URL·계정 전부 지정 (실습에서 많이 씀)

```bash
docker rm -f crud2-app 2>/dev/null

docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://crud2-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/crud2_db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME="admin" \
  -e SPRING_DATASOURCE_PASSWORD='본인RDS비밀번호' \
  crud2:latest
```

- `xxxxx` → 콘솔 **엔드포인트** 그대로
- `/crud2_db` → RDS **초기 DB 이름**과 동일해야 함
- 비밀번호에 `!`, `$` 있으면 **작은따옴표** `'...'`

### 방법 B — prod 파일에 URL 있을 때 (계정만 -e)

`application-prod.properties`에 URL이 이미 있으면:

```bash
docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD='본인RDS비밀번호' \
  crud2:latest
```

### 확인

```bash
docker ps
docker logs crud2-app
```

`Started Crud2Application`, `HikariPool` 관련 로그가 보이면 OK.

---

## 9단계 — 브라우저 + 체크리스트

### 접속

```
http://EC2퍼블릭IP:8080/list
```

예: `http://3.36.101.101:8080/list`

### 체크리스트

- [ ] RDS **사용 가능**
- [ ] EC2 SG: 22, 8080
- [ ] RDS SG: **3306 ← EC2 SG**
- [ ] EC2·RDS **같은 VPC**
- [ ] SSH 성공 (`ubuntu` 또는 `ec2-user`)
- [ ] `docker ps` → `crud2-app` Up
- [ ] `/list` 화면 표시

### 안 될 때

| 증상 | 확인 |
|------|------|
| SSH publickey | 키 페어 = pem, AMI 사용자명 |
| 브라우저 안 됨 | EC2 8080, `docker ps` |
| DB 연결 실패 | RDS 3306←EC2 SG, URL `/crud2_db`, 계정·비번 |
| Unknown database | RDS에 `crud2_db` 생성 여부 |

### 재배포

로컬: `docker build` → `docker save` → `scp`  
EC2: `docker load` → `docker rm -f crud2-app` → `docker run` (8단계)

---

## 부록 A — EC2에서 RDS 접속 테스트 (MySQL 클라이언트)

앱 전에 RDS가 EC2에서 보이는지 확인할 때 사용합니다.

### Amazon Linux

```bash
sudo dnf install -y mariadb105

mysql -h crud2-db.xxxxx.ap-northeast-2.rds.amazonaws.com -u admin -p
```

비밀번호 입력 후 `SHOW DATABASES;` → `crud2_db` 확인.

### Ubuntu

```bash
sudo apt install -y mysql-client

mysql -h crud2-db.xxxxx.ap-northeast-2.rds.amazonaws.com -u admin -p
```

> 여기서도 안 되면 **RDS 보안 그룹 3306 ← EC2 SG** 를 먼저 고칩니다.

---

## 부록 B — docker run 한 줄 예시 (복사용)

엔드포인트·비밀번호만 바꿔서 사용:

```bash
docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://crud2-db.cnau2o8em3i3.ap-northeast-2.rds.amazonaws.com:3306/crud2_db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul" \
  -e SPRING_DATASOURCE_USERNAME="admin" \
  -e SPRING_DATASOURCE_PASSWORD='admin1234' \
  crud2:latest
```

> `admin1234`는 **예시**입니다. 실제 RDS 비밀번호로 바꾸고, Git에 올리지 마세요.

---

## 부록 C — prod vs docker 프로파일

| 프로파일 | 용도 | DB |
|----------|------|-----|
| `prod` | EC2 + RDS | AWS RDS |
| `docker` | 로컬 `docker compose` | MySQL 컨테이너 |

EC2 실습은 **`SPRING_PROFILES_ACTIVE=prod`** 만 사용합니다.

---

## 참고 문서

| 문서 | 내용 |
|------|------|
| [`집에서하기-RDS-만들기-설정.md`](./집에서하기-RDS-만들기-설정.md) | RDS 1편 |
| [`집에서하기-EC2-Docker-RDS-1to9.md`](./집에서하기-EC2-Docker-RDS-1to9.md) | 1~9 요약 |
| [`ec2-docker-rds.md`](./ec2-docker-rds.md) | 상세 |

---

*문서 위치: `crud2/docs/집에서하기-2-EC2-Docker-RDS.md`*
