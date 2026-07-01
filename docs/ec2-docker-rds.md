# EC2 + Docker + RDS — 실제 서비스에 가까운 구성 (순서별)

Spring Boot(`crud2`)를 **Docker**로 올리고 **Amazon RDS MySQL**에 붙이며, **EC2**에서 실행할 때의 흐름을 단계별로 정리한 문서입니다.

---

## 1. 네트워크·보안을 먼저 그리기 (개념)

- **RDS**: 인터넷에 직접 열지 않고, **EC2(앱)**에서만 **3306**으로 접속하게 하는 것이 일반적입니다.
- **EC2**: 사용자는 **80/443**(또는 **8080**)으로만 접속합니다.

즉, **보안 그룹**은 대략 다음처럼 맞춥니다.

| 대상 | 인바운드 규칙 (요약) |
|------|----------------------|
| **RDS** | **3306** ← **EC2 보안 그룹**만 허용 |
| **EC2** | **22(SSH)** ← 본인 IP만 (또는 베스천), **80/443** ← `0.0.0.0/0` (또는 ALB만) |

VPC/서브넷은 RDS를 만들 때 이미 잡혀 있을 수 있습니다. 처음에는 **RDS와 EC2를 같은 VPC**에 두는 것이 단순합니다.

---

## 2. EC2 만들기

| 항목 | 권장 |
|------|------|
| AMI | Amazon Linux 2023 등 |
| 인스턴스 타입 | t3.micro 등 (트래픽·비용에 맞게) |
| 키 페어 | 생성·다운로드 (SSH용) |
| 보안 그룹 | 위에서 말한 대로 SSH + HTTP(HTTPS) 규칙 |
| IP | 퍼블릭 IP 또는 Elastic IP (고정 주소가 필요하면) |
| VPC | **RDS와 같은 VPC**인지 확인 |

---

## 3. EC2에 Docker 설치

SSH 접속 후 (Amazon Linux 2023 예시):

```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

로그아웃 후 다시 로그인하거나, `newgrp docker`로 docker 그룹을 적용합니다.

---

## 4. 앱 이미지를 EC2로 가져오기 (택일)

### A. ECR 사용 (운영에 흔함)

1. 로컬에서 `docker build` → **ECR에 push**
2. EC2에서 `aws ecr get-login-password` → `docker pull`

### B. 간단히 테스트만

- 로컬에서 `docker save` → `scp` → EC2에서 `docker load`  
- 이미지가 크면 비효율적일 수 있습니다.

실서비스 구조라면 **A(ECR)**를 추천합니다.

---

## 5. RDS 정보로 컨테이너 실행

EC2에서 (값은 본인 환경에 맞게 수정):

```bash
docker run -d -p 80:8080 --name crud2-app \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://<RDS엔드포인트>:3306/<DB이름>?characterEncoding=UTF-8&serverTimezone=Asia/Seoul' \
  -e SPRING_DATASOURCE_USERNAME='...' \
  -e SPRING_DATASOURCE_PASSWORD='...' \
  <ECR_URI>/crud2:latest
```

- **포트**: `80:8080`이면 브라우저에서 `http://<EC2퍼블릭IP>/`로 접속 (앱이 8080을 쓸 때).
- **crud2**는 루트 매핑이 없을 수 있으므로 **`http://<EC2퍼블릭IP>/list`** 로 확인하는 것이 안전합니다.
- 비밀번호는 나중에 **Secrets Manager** / **SSM Parameter Store**로 분리하는 것을 권장합니다.

---

## 6. 동작 확인

1. EC2에서: `docker ps`, `docker logs crud2-app`
2. PC 브라우저: `http://<퍼블릭IP>/list`
3. 안 되면 확인할 것:
   - RDS 보안 그룹이 **EC2 보안 그룹**에서 오는 **3306**을 허용하는지
   - JDBC URL, DB 이름, 계정·비밀번호가 맞는지

---

## 7. “실제 서비스”에 가까워지는 다음 단계 (순서 추천)

| 순서 | 내용 |
|------|------|
| 1 | 도메인 연결 (Route 53) |
| 2 | HTTPS: ALB + ACM 인증서, 또는 EC2 앞에 ALB |
| 3 | Nginx 리버스 프록시 (선택) |
| 4 | 로그·모니터링: CloudWatch에 컨테이너 로그 |
| 5 | 배포 자동화: GitHub Actions → ECR push → EC2/ECS 배포 |
| 6 | 트래픽이 많아지면 **ECS Fargate + ALB + RDS**로 이전 검토 |

---

## 한 줄 요약

**다음 순서:**  
EC2 생성 → Docker 설치 → (이미지는 ECR push 후 pull) → `docker run`으로 RDS URL·계정을 넣어 기동 → 보안 그룹·포트 확인 → 도메인·HTTPS·로그·자동 배포 순으로 다듬습니다.

**1차 목표:** EC2에 Docker를 깔고, ECR에서 이미지를 받아 **prod 환경 변수로 RDS에 붙인 컨테이너**를 실행하는 것까지입니다.

---

## 관련 문서

- 같은 프로젝트: [aws-docker-mysql-deploy.md](./aws-docker-mysql-deploy.md), [docker-deployment.md](./docker-deployment.md)
