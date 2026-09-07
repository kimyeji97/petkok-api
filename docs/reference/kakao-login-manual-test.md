# 카카오 로그인 수동 왕복 — petkok access 토큰 받는 절차

> 용도: 로컬 실측(bootRun + curl)에 쓸 **petkok access 토큰**을 손으로 받는 절차. `./gradlew test`는 DB도 카카오도
> 타지 않으므로(`CLAUDE.local.md`), keyset 경계·자동가입 같은 것은 이 절차로 토큰을 받은 뒤 curl로 확인한다.
> 출처: `docs/PROGRESS.md` 2026-07-29(콘솔 왕복) · 2026-08-07(첫 서버 왕복) · 2026-09-03(REQ-10 실측 계획)과
> `KakaoOAuthClient`·`AuthController` 코드. 2026-09-04 정리.
>
> **자동화하지 않는 이유** — 인가코드는 **사람이 브라우저로 로그인해야** 나오고, **1회용에 약 10분 만료**다.
> 그래서 REQ-07 계획서도 이 왕복을 "수동 확인 항목"으로 남겼다(`PLAN-REQ-07` §제약).

## 0. 사전 확인 — 여기서 막히면 뒤가 전부 헛돈다

| 확인 | 방법 | 안 되면 |
| --- | --- | --- |
| `.env`에 Kakao 3개가 **주석 해제**되어 실값이 있다 | `/usr/bin/grep -cE '^KAKAO_[A-Z_]+=' .env` → `2` 또는 `3` | `0`이면 앱이 `client-id: dummy`로 뜬다. 카카오가 `KOE101`(client_id 없음)로 거부하고 **키 문제로 오진**하기 쉽다. `.env.example` §Kakao 대로 채운다. ⚠️ `KEY=`(빈 값)은 기본값을 무력화하므로 `CLIENT_SECRET`은 콘솔에서 "사용함"이 아니면 **줄을 주석으로 둔다** |
| 로컬 Postgres가 떠 있다 | `docker ps --format '{{.Names}} {{.Status}}'`에 `petkok-pg` | `docker start petkok-pg` |
| 콘솔 Redirect URI가 `.env`의 `KAKAO_REDIRECT_URI`와 **문자 단위로** 같다 | developers.kakao.com → 제품 설정 → 카카오 로그인 → Redirect URI | 다르면 토큰 교환이 `KOE006`/`invalid_grant`. 기본값은 `http://localhost:3000/oauth/kakao/callback` |
| 콘솔 「허용 IP 주소」가 **비어 있거나** 지금 공인 IP가 등록돼 있다 | 앱 설정 → 플랫폼/보안 | 값이 하나라도 있으면 allowlist가 켜진다. 토큰 교환은 되는데 `/v2/user/me`만 `-401 ip mismatched!` |
| 동의항목 `profile_nickname`이 켜져 있다 | 카카오 로그인 → 동의항목 | 꺼져 있으면 `users.nickname NOT NULL`에 걸려 자동가입 실패(502) |

`.env`의 `KAKAO_*` 값은 이 문서·`PROGRESS.md`·대화 어디에도 옮겨 적지 않는다. REST API 키도 마찬가지다.

## 1. 앱 기동

```bash
./gradlew bootRun > /tmp/petkok-bootrun.log 2>&1 &
# .env 는 Spring 이 자동으로 읽는다 (set -a 불필요)
curl -s localhost:8080/actuator/health        # {"status":"UP"} 이 나올 때까지
```

macOS에는 `timeout`이 없으므로 위처럼 백그라운드 + 로그 파일로 돈다(`CLAUDE.md` §로컬 검증).

## 2. 브라우저에서 인가코드 받기 (사람 개입 지점)

아래 URL을 브라우저에 붙여 넣는다. `client_id`는 **REST API 키**, `redirect_uri`는 `.env` 값과 같아야 한다.

```text
https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=<REST API 키>&redirect_uri=http://localhost:3000/oauth/kakao/callback
```

로그인·동의를 마치면 브라우저가 아래 주소로 이동한다.

```text
http://localhost:3000/oauth/kakao/callback?code=XXXXXXXX
```

**3000번에 아무 서버도 없어 "연결할 수 없음" 페이지가 뜬다. 정상이다.** 서버는 리다이렉트를 받지 않는 커스텀 플로우라
필요한 건 **주소창의 `code=` 뒤 값**뿐이다. 그것을 복사한다.

- 이 코드는 **1회용, 약 10분 만료.** 3단계에서 실패하면 다시 여기로 돌아와 새로 받는다
- 이미 로그인된 브라우저라면 동의 화면 없이 바로 리다이렉트된다. 코드는 그래도 새로 발급된다

## 3. petkok 토큰으로 교환

```bash
CODE='<2단계에서 복사한 값>'
curl -s -X POST localhost:8080/api/v1/auth/kakao \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"$CODE\"}"
```

성공 응답(200) — 카카오 토큰이 아니라 **petkok이 발급한** JWT다.

```json
{"data":{"access_token":"eyJ…","refresh_token":"eyJ…"},"error":null}
```

토큰은 셸 변수나 스크래치패드 파일에만 둔다. 레포 안 파일·문서·대화에 붙이지 않는다.

```bash
ACCESS='<access_token 값>'
```

이 한 번의 호출로 서버가 **인가코드 → 토큰 교환(`kauth`) → 프로필(`kapi`) → 자동가입 → JWT 발급**을 전부 돈다.
처음 로그인하는 카카오 계정이면 `users` 행이 이때 생긴다.

## 4. 토큰이 사는지 확인

```bash
curl -s localhost:8080/api/v1/users/me -H "Authorization: Bearer $ACCESS"
```

`data.nickname`이 카카오 닉네임으로 오면 끝. 이후 실측(펫 생성·기록 생성·keyset 조회)은 같은 헤더로 이어서 한다.

- access 토큰 유효기간은 **30분**(`JWT_ACCESS_TTL` 기본값). 실측이 길어지면 `POST /api/v1/auth/refresh`에
  `{"refresh_token":"…"}`로 재발급한다. **응답의 새 refresh 토큰으로 반드시 교체**한다 — 옛 것을 다시 보내면
  재사용 감지로 전 기기 로그아웃된다
- 카카오에 다시 로그인할 필요는 없다. 2단계는 refresh 토큰까지 만료됐을 때만 반복한다

## 5. 실패 진단 — 증상별로 어디를 보나

로그(`/tmp/petkok-bootrun.log`)에 카카오 오류 본문이 남는다. `code`·`client_id`·토큰은 `앞4자+***`로 마스킹되지만
`error_code`·`msg` 같은 진단값은 그대로 찍힌다(REQ-07 Phase 4 결정).

| 증상 | 원인 | 조치 |
| --- | --- | --- |
| 400 `INVALID_INPUT` | `code`가 비었거나 JSON 키 이름이 다르다 | 요청 본문 확인 |
| 502 `EXTERNAL_API_ERROR` + 로그 `KOE320` / `invalid_grant` | 인가코드 **재사용 또는 만료** | 2단계로 돌아가 새 코드 |
| 502 + 로그 `KOE006` / `invalid_grant` (redirect) | `redirect_uri` 불일치 — 콘솔·`.env`·인가 URL 셋 중 하나가 다르다 | 세 값을 문자 단위로 대조 |
| 502 + 로그 `KOE101` (client_id 없음) | `.env`의 `KAKAO_CLIENT_ID`가 주석 상태라 `dummy`로 나갔거나 키 종류가 틀렸다(네이티브/JS 키) | §0 첫 줄 |
| 502 + 로그 `KOE010` (bad client credentials) | 콘솔 "사용함"인데 `KAKAO_CLIENT_SECRET`이 비었거나 다르다 | `.env` 확인 |
| 502 + 로그 `{"code":-401,"msg":"ip mismatched!"}` | **토큰 교환은 성공**했고 `kapi`만 막힌 것 — 키가 아니라 콘솔 허용 IP | 허용 IP 비우거나 현재 IP 등록. **토큰이 발급됐다면 키 3개는 정상**이라는 진단 규칙(2026-07-29) |
| 502 + 로그 `no nickname` | 동의항목 `profile_nickname` 꺼짐 | 콘솔에서 켜고 재로그인 |
| 4단계에서 401 | access 토큰 만료(30분) 또는 헤더 오타 | `/auth/refresh` |
| 카카오는 200인데 502 | 응답 필드 매핑(`@JsonProperty`) 회귀. 2026-08-07에 한 번 났다 | `KakaoTokenResponse`·`KakaoUserResponse` 확인, REQ-07-24 |

**한 번 실패할 때마다 코드를 새로 받아야 한다.** 3단계 뒤에 붙일 스크립트가 있으면 **가짜 값으로 전 경로를 먼저 드라이런**한 뒤
실물 코드를 넣는다 — 성공 경로에만 있던 버그 하나로 코드를 두 번 태운 적이 있다(2026-07-29).

## 6. 정리

```bash
kill %1                                      # bootRun 백그라운드 잡
unset ACCESS CODE
```

이 계정으로 만든 `users`·`pets`·기록 행은 로컬 DB(`petkok_local`)에 남는다. 다음 실측에서 같은 계정으로 로그인하면
자동가입 없이 같은 `users` 행으로 들어온다. 초기화가 필요하면 `CLAUDE.local.md` 「완전 초기화」.
