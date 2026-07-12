# Export 샘플 JSON 생성 기준

이 문서는 `LogExporter`가 생성하는 export JSON(`schema_version=6`)의 **샘플 작성 기준**을 정리한 문서입니다.
샘플 파일은 실제 사용자 데이터를 복제하지 않고, **필드 구조와 생성 규칙을 검증하기 위한 구조 예시**로만 사용합니다.

## 1. 기본 원칙
- 실제 export와 동일한 top-level key 순서를 유지합니다.
- 값은 구조 예시용이며, 실제 사용자 데이터/실제 로그를 포함하지 않습니다.
- timestamp 계열은 예시 값 사용이 가능하지만, 실제 export에서는 `System.currentTimeMillis()`와 ISO 문자열을 함께 기록합니다.
- 데이터가 없는 배열(`threat_scans`, `media_insights`, `device_photos`, `receipt_memories`)은 빈 배열 `[]`을 사용합니다.
- 필수 중첩 객체(`settings`, `gemini_nano_connector`, `provider`, `session_factory`, `inference_trace`)는 항상 객체 형태를 유지합니다.

## 2. top-level 필드 생성 규칙
- `schema_version`: `LogExporter.SCHEMA_VERSION` 값을 그대로 사용합니다. 현재 값은 `6`입니다.
- `exported_at`: export 실행 시점의 epoch millis
- `exported_at_iso`: export 실행 시점의 ISO 문자열
- `app_profile`: `model_manifest.json`의 `appProfile`
- `settings`: `AppPreferencesStore.getSettings()`에서 읽은 설정 스냅샷
- `gemini_nano_connector`: `OnDeviceRuntimeRegistry`의 connector 진단 정보
- `runtime_diagnostics`: 런타임 가용성 진단 배열
- `runtimes`: manifest의 runtime 정의 배열
- `threat_scans`: Room threat scan 이력 배열
- `media_insights`: Room media insight 이력 배열
- `device_photos`: Room device photo snapshot 배열
- `receipt_memories`: Room receipt memory 배열

## 3. settings 블록 기준
다음 필드를 포함합니다.
- `spam_filter_mode`
- `preferred_runtime`
- `gemini_nano_connector_mode`
- `auto_index_after_permission_grant`

## 4. gemini_nano_connector 블록 기준
다음 필드를 포함합니다.
- `mode`
- `mode_label`
- `available`
- `status`
- `binding_phase`
- `contract_summary`
- `detail`
- `provider`
  - `status`
  - `summary`
  - `layer`
- `session_factory`
  - `status`
  - `summary`
  - `layer`
  - `inference_entry_point`
  - `output_expectation`
  - `fallback_policy`
- `todo_checklist`
- `inference_trace`
  - `schema`
  - `buffered`
  - `capacity`
  - `last_label`
  - `last_latency_ms`
  - `records`

## 5. inference_trace.records 기준
trace record는 존재할 때만 배열에 채우며, 각 원소는 아래 필드를 가집니다.
- `trace_id`
- `request_id`
- `session_label`
- `connector_mode`
- `provider_label`
- `factory_label`
- `input_preview`
- `input_char_length`
- `input_token_estimate`
- `hit_signals`
- `link_detected`
- `decision_rule`
- `output_label`
- `output_score`
- `output_logits`
- `output_confidence_band`
- `started_at`
- `completed_at`
- `latency_ms`
- `fallback_reason` (nullable, 있을 때만 포함)

## 6. 컬렉션 샘플 작성 기준
- `threat_scans`: 각 항목은 스캔 결과 1건을 의미하며, `created_at`과 `created_at_iso`를 함께 유지합니다.
- `media_insights`: `labels`는 배열, `receipt_like`/`screenshot_like`/`document_like`는 boolean을 사용합니다.
- `device_photos`: `content_uri`는 예시 URI를 사용하되 실제 사용자 URI를 넣지 않습니다.
- `receipt_memories`: `raw_text`는 구조 예시용 짧은 문자열을 사용합니다.

## 7. 파일명 기준
실제 export 파일명은 아래 패턴을 따릅니다.
- `privacy_first_ai_log_yyyyMMdd_HHmmss.json`

## 8. 샘플 파일 사용 목적
- QA에서 JSON 파서/뷰어 회귀 테스트
- schema 변경 시 diff 기준점 확보
- 외부 문서화/샘플 첨부용 참조 구조 제공
