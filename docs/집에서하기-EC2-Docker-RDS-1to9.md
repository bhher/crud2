# crud2 — 집에서 하기 (EC2 + Docker + RDS) 1~9단계

Ubuntu EC2에 Docker로 crud2를 올리고 RDS MySQL(`crud-db`)에 연결하는 **전체 순서**입니다.

> **RDS가 없으면 먼저:** [`집에서하기-RDS-만들기-설정.md`](./집에서하기-RDS-만들기-설정.md) (RDS 생성 ~ Spring 연결)  
> **실습·복습본:** [`집에서하기-2-EC2-Docker-RDS.md`](./집에서하기-2-EC2-Docker-RDS.md)  
> **사전 완료:** RDS `crud-db` 생성, `application-prod.properties`에 JDBC URL 설정됨  
> **접속 URL 예:** `jdbc:mysql://crud-db.cnau2o8em3i3.ap-northeast-2.rds.amazonaws.com:3306/crud2_db?...`

---

## 전체 그림

```
[내 PC 브라우저] ──8080──▶ [Ubuntu EC2 + Docker crud2] ──3306──▶ [RDS crud-db]
```

---

## 1단계 — AWS 준비 확인

### RDS (`crud-db`)

| 항목 | 확인 |
|------|------|
| 상태 | **사용 가능** |
| 엔드포인트 | 연결 및 보안 탭에서 복사 |
| DB 이름 | `crud2_db` (없으면 MySQL에서 `CREATE DATABASE crud2_db;`) |
| 퍼블릭 액세스 | **아니요** (EC2만 붙이면 됨) |
| 마스터 사용자명·비밀번호 | 메모 (docker run 때 사용) |

### 로컬 파일

| 파일 | 위치 |
|------|------|
| 프로젝트 | `d:\spring1\crud2` |
| SSH 키 | `crud2-key.pem` (EC2 만들 때 선택한 키와 **이름 동일**) |
| prod 설정 | `src/main/resources/application-prod.properties` (RDS URL 포함) |

---

## 2단계 — EC2 인스턴스 만들기 (Ubuntu)

1. **EC2** → **인스턴스 시작**
2. 설정:

| 항목 | 값 |
|------|-----|
| 이름 | `crud2-server` |
| AMI | **Ubuntu Server 22.04 LTS** (또는 24.04) |
| 인스턴스 유형 | `t3.micro` (프리 티어) |
| 키 페어 | **기존 `crud2-key`** 또는 새로 생성 후 **`.pem` 다운로드** |
| VPC | **RDS와 동일 VPC** |
| 퍼블릭 IP | **활성화** |

3. **보안 그룹** (예: `launch-wizard-1`) — 인바운드:

| 유형 | 포트 | 원본 | 비고 |
|------|------|------|------|
| SSH | 22 | **내 IP** | 터미널 접속 |
| 사용자 지정 TCP | **8080** | **내 IP** (학습용 `0.0.0.0/0` 가능) | 웹 — **원본에 8080 넣지 말 것** |

4. **인스턴스 시작** → **퍼블릭 IPv4** 복사 (예: `54.116.218.196`)

5. EC2 **보안 그룹 ID** 메모 (`sg-xxxx`) — RDS 3단계에서 사용

---

## 3단계 — RDS 보안 그룹 (3306)

1. **RDS** → `crud-db` → **연결 및 보안** → **VPC 보안 그룹** 클릭
2. **인바운드 규칙 편집** → **규칙 추가**

| 유형 | 포트 | 원본 |
|------|------|------|
| MYSQL/Aurora | 3306 | **EC2 보안 그룹** (`launch-wizard-1` 의 `sg-xxxx`) |

- 원본은 **`sg-...` ID** 또는 보안 그룹 이름 검색
- ❌ `0.0.0.0/0` (인터넷 전체) 넣지 않기
- ❌ 원본 칸에 `8080` 넣지 않기

3. **규칙 저장**

---

## 4단계 — pem 권한 + SSH 접속

### Ubuntu SSH 사용자

| AMI | 사용자 |
|-----|--------|
| Ubuntu | **`ubuntu`** |
| Amazon Linux | `ec2-user` |

### Windows — pem 권한 (SSH 전에 1회)

PowerShell:

```powershell
icacls "d:\spring1\crud2\crud2-key.pem" /inheritance:r
icacls "d:\spring1\crud2\crud2-key.pem" /grant:r "$($env:USERNAME):(F)"
icacls "d:\spring1\crud2\crud2-key.pem" /remove "Authenticated Users"
icacls "d:\spring1\crud2\crud2-key.pem" /remove "Users"
```

`UNPROTECTED PRIVATE KEY` 나오면 위 명령 다시 실행 (pem **파일마다** 필요).

### SSH 접속

```powershell
ssh -i "d:\spring1\crud2\crud2-key.pem" ubuntu@EC2퍼블릭IP
```

- 처음: `yes` 입력
- `Permission denied (publickey)` → EC2 **키 페어 이름**과 pem 짝 확인
- `ssh -v` 에 `type -1` → pem 권한 문제 → icacls 또는 WSL 사용:

```bash
# WSL 대안
cp /mnt/d/spring1/crud2/crud2-key.pem ~/crud2-key.pem
chmod 400 ~/crud2-key.pem
ssh -i ~/crud2-key.pem ubuntu@EC2퍼블릭IP
```

접속 성공: `ubuntu@ip-172-31-xx-xx:~$`

---

## 5단계 — EC2에 Docker 설치 (Ubuntu)

EC2 SSH 접속된 터미널에서:

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
```

**로그아웃 후 다시 SSH** (`exit` → ssh 다시).

확인:

```bash
docker --version
```

---

## 6단계 — 로컬 PC에서 이미지 빌드

**새 PowerShell** (로컬 PC):

```powershell
cd d:\spring1\crud2
docker build -t crud2:latest .
docker save crud2:latest -o crud2-image.tar
```

- `application-prod.properties` 수정 후에는 **반드시 다시 build**
- `docker build` 완료까지 수 분 소요

---

## 7단계 — 이미지를 EC2로 전송

로컬 PowerShell:

```powershell
scp -i "d:\spring1\crud2\crud2-key.pem" d:\spring1\crud2\crud2-image.tar ubuntu@EC2퍼블릭IP:~/
```

파일 크기에 따라 몇 분 걸릴 수 있음.

---

## 8단계 — EC2에서 이미지 로드 + 실행 (RDS 연결)

EC2 SSH에서:

```bash
docker load -i crud2-image.tar
docker images
```

컨테이너 실행 (**RDS 사용자명·비밀번호**로 교체):

```bash
docker rm -f crud2-app 2>/dev/null

docker run -d --name crud2-app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_USERNAME=RDS마스터사용자명 \
  -e SPRING_DATASOURCE_PASSWORD='RDS비밀번호' \
  crud2:latest
```

- JDBC URL은 `application-prod.properties`에 있음 → `-e SPRING_DATASOURCE_URL` 불필요
- 비밀번호 특수문자 있으면 작은따옴표 `'...'` 사용

확인:

```bash
docker ps
docker logs crud2-app
```

`Started Crud2Application` 비슷한 로그가 보이면 OK.

---

## 9단계 — 브라우저 접속 + 최종 체크

### 접속

```
http://EC2퍼블릭IP:8080/list
```

게시판 목록 보이면 **배포 + RDS 연결 성공**.

### 체크리스트

- [ ] EC2 실행 중, 퍼블릭 IP 확인
- [ ] EC2 SG: 22, **8080** (내 IP)
- [ ] RDS SG: **3306 ← EC2 SG** (`sg-...`)
- [ ] SSH: `ubuntu@IP` + 맞는 pem
- [ ] `docker ps` → crud2-app Up
- [ ] `/list` 페이지 열림

### 안 될 때

| 증상 | 확인 |
|------|------|
| SSH `publickey` | 키 페어 이름 = pem, 사용자 `ubuntu`, icacls |
| SSH `type -1` | pem 권한 또는 WSL `chmod 400` |
| 브라우저 안 열림 | EC2 SG 8080, `docker ps` |
| DB 연결 실패 | RDS SG 3306←EC2 SG, VPC 동일, 계정/비번, `crud2_db` |
| `docker logs` 에러 | 로그 전문 확인 |

### 코드 수정 후 재배포

로컬: `docker build` → `docker save` → `scp`  
EC2: `docker load` → `docker rm -f crud2-app` → `docker run` (8단계 반복)

---

## 참고 문서

| 문서 | 내용 |
|------|------|
| `docs/ec2-docker-rds.md` | EC2+RDS 상세 |
| `docs/crud2-docker-image.md` | Docker 이미지 빌드 |
| `docs/aws-docker-mysql-deploy.md` | AWS 전체 흐름 |

---

*작성: 집에서 이어하기용 1~9단계 요약*
