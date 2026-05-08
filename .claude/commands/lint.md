Kotlin 린트/포맷을 실행한다 (kotlinter 기반).

## 절차

1. 인자 확인:
   - `--check` → `./gradlew lintKotlin` (검사만, 자동 수정 X)
   - 인자 없음 / `--fix` → `./gradlew formatKotlin` (자동 수정 후 잔여 lintKotlin)
   - 모듈 인자 → 해당 모듈에 한정 (`./gradlew :lab-space:app:formatKotlin`)
2. `formatKotlin` 후 변경된 파일이 있으면 사용자에게 어떤 파일이 수정됐는지 알려준다 — 의도하지 않은 변경을 사용자가 검토할 수 있게.
3. lint 실패가 남으면:
   - 룰 이름·위치를 그대로 전달.
   - 자동 수정 불가능한 룰이면 직접 수정하거나 사용자에게 위임.
4. 성공 시 한 줄 요약.

## 룰

- pre-commit hook 이 같은 동작을 하므로, `/commit` 직전에 자체적으로 한번 도는 셈. 그래도 명시적 호출은 빠른 피드백 용도로 의미가 있다.
- `@Suppress` 를 임의로 추가해 룰을 우회하지 않는다 (`conventions.md` 기본 원칙).
