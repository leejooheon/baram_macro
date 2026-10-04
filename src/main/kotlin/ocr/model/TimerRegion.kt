package ocr.model

import java.awt.Rectangle

/**
 * OCR로 읽는 화면 영역. 좌표는 java.awt.Robot 기준(윈도우 배율이 적용된 논리 좌표)이다.
 * 기본값은 2560x1600 화면, 배율 150%, 게임 창 최대화 상태에서 잡은 값이라 영역 지정으로 맞춰 쓴다.
 */
enum class TimerRegion(
    val title: String,
    val defaultRectangle: Rectangle,
) {
    /** 우상단 검은 박스 (예: 호체주술 243초) */
    COOLDOWN("스킬 쿨타임", Rectangle(1265, 58, 200, 46)),

    /** 우측 가운데 양피지 패널 (예: 호체주술 49초 / 보호 178초 / 무장 179초) */
    BUFF("버프", Rectangle(1305, 446, 285, 122)),
}
