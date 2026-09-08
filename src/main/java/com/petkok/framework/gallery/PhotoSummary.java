package com.petkok.framework.gallery;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@link PhotoLookup} 이 돌려주는 사진 요약. {@code data.gallery.entity.Photo} 를 그대로 넘기지 않는다 — {@code
 * petId}·{@code diaryEntryId} 는 호출부(다이어리)가 이미 알고 있어 뺐다 (PLAN-REQ-11 결정 · Phase 0).
 *
 * <p>{@code dto} 패키지가 아니라 {@link PhotoLookup} 과 같은 위치에 둔 것도 의도적이다 — {@code ..dto..} 에 두면 {@code
 * DTO_NAMING} 규칙({@code Request}/{@code Response} 로 끝나야 함)에 걸린다.
 */
public record PhotoSummary(UUID id, String imageUrl, String caption, LocalDate takenAt) {}
