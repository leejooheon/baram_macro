package ocr.model

import java.awt.geom.Rectangle2D

/**
 * OCR로 읽는 영역. 게임 창 클라이언트 영역에 대한 비율(0~1)로 저장해서 창 크기가 바뀌어도 따라간다.
 * 기본값은 게임 창을 2560x1600 화면에 최대화한 스크린샷에서 잡은 값이다.
 */
enum class TimerRegion(
    val title: String,
    val defaultFraction: Rectangle2D.Double,
) {
    /** 우상단 검은 박스 (예: 호체주술 243초). 여러 마법이 쿨타임이면 아래로 줄이 쌓이므로 박스 전체(약 4~5줄)를 잡는다 */
    COOLDOWN("스킬 쿨타임", Rectangle2D.Double(0.7432, 0.0369, 0.1175, 0.19)),

    /** 우측 가운데 양피지 패널 (예: 호체주술 49초 / 보호 178초 / 무장 179초) */
    BUFF("버프", Rectangle2D.Double(0.7655, 0.4259, 0.1703, 0.1288)),
}
