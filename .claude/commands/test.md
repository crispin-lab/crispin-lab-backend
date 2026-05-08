테스트를 실행한다.

## 절차

1. 인자 확인:
   - 모듈 인자(예: `:lab-space:domain`) → 해당 모듈만 `./gradlew :lab-space:domain:test`
   - 클래스 패턴(예: `*PageGetting*`) → `./gradlew test --tests "*PageGetting*"` (필요 시 모듈 한정)
   - 인자 없음 → `./gradlew test` (전체)
2. 실패 시:
   - 어느 Spec, 어느 `describe > it` 에서 실패했는지 위치를 짚는다.
   - Kotest 출력의 stack 보다 `expected vs actual` 를 우선 요약.
3. 성공 시 통과 개수만 한 줄 요약.

## 룰

- 테스트 추가/수정 직후 자동 실행 권장 (`developer` 흐름과 결합 시).
- flaky 의심이 들면 `--rerun-tasks` 한 번만 시도, 그 이상은 사용자에게 위임.
- 외부 네트워크/DB 의존이 의심되면 `conventions.md` "외부 의존성은 mock/stub" 위반 — 코드 수정 권장으로 응답.
