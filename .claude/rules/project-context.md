# 프로젝트 컨텍스트

위키 + 블로그 하이브리드 (Confluence 스타일). 단일 bounded context를 중심으로 구성.

- `Space` — 컨테이너 (예: 자유게시판, 공지사항)
- `Page` — 컨텐츠. Space에 소속. `parentPageId`로 계층 구성, `PageRevision`으로 버전 관리, 위키 스타일 `[[...]]` 참조를 `PageLink`로 저장. `Comment`(별도 aggregate)와 `Tag`(다대다)를 가짐.

## 현재 스코프 외 (나중에)
- Elasticsearch 기반 검색 — 지금은 `PageSearchPort` 인터페이스만 정의하고 SQL `LIKE`로 구현. 나중에 ES 구현으로 교체.
