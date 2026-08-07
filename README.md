# GSM-verify

GSM Queue 서버용 Discord 인증 Paper 플러그인입니다.

## 기능

- `/verify` 명령으로 Discord OAuth 인증 링크 발급
- Discord guild 가입 여부 확인
- 졸업생/재학생 role 확인
- Discord 서버 프로필 이름 형식 확인
- 인증 정보 로컬 저장
- 인증 완료 후 BungeeCord plugin message로 SMP 이동
- 인증 완료/초기화 알림을 `gsm:notify` payload로 전달
- 매년 지정일 인증 정보 초기화

## 인증 기준

### 졸업생

- `graduateRoleId` role 보유
- Discord 서버 프로필 이름 형식에서 기수와 이름 추출
- 저장 필드: `roleType=GRADUATE`, `flag`, `name`

### 재학생

- `studentRoleId` role 보유
- Discord 서버 프로필 이름 형식에서 학번과 이름 추출
- 저장 필드: `roleType=STUDENT`, `studentId`, `name`

## 설정

`plugins/GSM-verify/config.yml`

```yaml
discord:
  clientId: ""
  clientSecret: ""
  redirectUri: "http://localhost:27073/callback"
  botToken: ""
  guildId: ""
  graduateRoleId: ""
  studentRoleId: ""
  announcementChannelId: ""

oauthServer:
  host: "0.0.0.0"
  port: 27073
  callbackPath: "/callback"

server:
  smpName: "smp"

verification:
  stateExpireSeconds: 300
  resetMonth: 1
  resetDay: 12
```

운영 환경에서는 Discord client secret과 bot token을 문서나 로그에 기록하지 않습니다.

## 데이터 파일

```text
plugins/GSM-verify/verified-users.yml
```

UUID 기준으로 인증 정보를 저장합니다.

## 명령어

```text
/verify
/verify reload
/verify status <player>
/verify reset <player>
/verify resetall confirm
```

## 권한

```text
gsmverify.admin
gsmverify.reload
gsmverify.status
gsmverify.reset
gsmverify.resetall
```

## Plugin Message

### BungeeCord 이동

인증 완료 후 `Connect`, `smp` plugin message를 전송해 플레이어를 SMP로 이동시킵니다.

### Queue Notify

- 채널: `gsm:notify`
- 버전: `1`
- 타입: `VERIFY_SUCCESS`, `VERIFY_RESET`
- 포함 정보: type, uuid, name, roleType, studentId, flag, timestamp

이 payload는 SMP의 `GSM-Grim` 인증 캐시와 `GSM-Discord` 인증 알림 임베드에서 사용합니다.

## 빌드

```bat
gradlew.bat build
```

산출물:

```text
build/libs/GSM-verify-1.0.0.jar
```

## 배포 위치

```text
servers/GSM-Queue/plugins/
```
