현재 변경사항에 대한 git 커밋을 만든다.

커밋 컨벤션 전문은 `.claude/rules/commit.md`에 있다 (CLAUDE.md에서 자동 import). 그 룰을 따른다.

## 절차
1. 병렬로 실행:
   - `git status` (`-uall` 옵션 사용 금지)
   - `git diff` (staged + unstaged 모두)
   - `git log --oneline -5` 로컬 스타일 확인
2. diff를 읽고 변경을 일관된 단위로 묶어 메시지 작성한다.
   - staged 변경에 무관한 관심사가 섞여 있으면 **하나의 커밋으로 강제하지 말고 사용자에게 어떻게 나눌지 묻는다.**
3. 파일은 이름으로 명시 스테이징한다 (`rules/commit.md`에 따라 `-A` / `.` 금지).
4. 시크릿 파일로 보이는 것(`.env`, `credentials*`, `*.pem`, `*.key`)은 스테이징하지 않는다. 사용자가 명시적으로 요청한 경우에만 경고 후 커밋한다.
5. HEREDOC으로 메시지를 넘겨 커밋한다.
6. 커밋 후 `git status`로 정상 반영을 확인한다.
