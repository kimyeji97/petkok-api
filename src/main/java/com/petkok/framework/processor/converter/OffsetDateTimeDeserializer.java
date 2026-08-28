package com.petkok.framework.processor.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * 요청 시각 문자열 → {@link OffsetDateTime}. 오프셋이 있으면 그 순간 그대로, <b>없으면 KST 벽시계로</b> 읽는다 (REQ-16 D9).
 *
 * <p>⚠️ <b>Jackson 기본 역직렬화는 오프셋 없는 값을 아예 거부한다.</b> 이 클래스가 없으면 {@code "2026-06-30T18:00:00"} 이 컨트롤러에
 * 도달하지 못하고 400 이 된다 — 검증 계약 REQ-16-15.
 *
 * <p>⚠️ <b>{@code timeZone(...)} 설정으로는 이 동작을 얻을 수 없다.</b> 그 설정은 <b>직렬화</b>의 렌더 기준일 뿐, 오프셋이 없는 입력을
 * 해석해 주지 않는다. 응답 쪽(REQ-16-08)과 요청 쪽(REQ-16-15)은 서로 다른 두 장치가 필요하다 — 한쪽만 보고 "설정 하나로 끝났다"고 읽지 말 것.
 *
 * <p>파싱은 {@link DateTimeFormatter#ISO_DATE_TIME} 으로 한다. {@code ISO_OFFSET_DATE_TIME} 은 오프셋을
 * <b>필수</b>로 요구해 D9 를 만족할 수 없다.
 */
public class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

  /** ⚠️ Phase 3 에서 {@code framework/constant} 의 {@code ZoneId} 상수로 옮긴다 (REQ-16-11). */
  private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Seoul");

  @Override
  public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String text = parser.getText();
    if (text == null || text.isBlank()) {
      return null;
    }
    TemporalAccessor parsed =
        DateTimeFormatter.ISO_DATE_TIME.parseBest(
            text.trim(), OffsetDateTime::from, LocalDateTime::from);
    if (parsed instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime;
    }
    return ((LocalDateTime) parsed).atZone(FALLBACK_ZONE).toOffsetDateTime();
  }
}
