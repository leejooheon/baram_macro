package ocr.model

import java.awt.geom.Rectangle2D

/**
 * 게임 창에서 읽는 영역. 게임 창 클라이언트 영역에 대한 비율(0~1)로 저장해서 창 크기가 바뀌어도 따라간다.
 * 기본값은 게임 창을 2560x1600 화면에 최대화한 스크린샷에서 잡은 값이다.
 */
enum class TimerRegion(
    val title: String,
    val defaultFraction: Rectangle2D.Double,
    val reader: Reader = Reader.OCR,
    /** 영역 지정 화면에 띄우는 안내 */
    val pickHint: String = "$title 글자가 보이는 곳을 드래그하세요. 점선은 지금 쓰는 영역이에요.",
) {
    /** 우상단 검은 박스 (예: 호체주술 243초). 여러 마법이 쿨타임이면 아래로 줄이 쌓이므로 박스 전체(약 4~5줄)를 잡는다 */
    COOLDOWN("스킬 쿨타임", Rectangle2D.Double(0.7432, 0.0369, 0.1175, 0.19)),

    /** 우측 가운데 양피지 패널 (예: 호체주술 49초 / 보호 178초 / 무장 179초) */
    BUFF("버프", Rectangle2D.Double(0.7655, 0.4259, 0.1703, 0.1288)),

    /**
     * 우하단 상태 패널의 체력(빨강)/마력(파랑) 막대 두 줄. 막대가 찬 폭으로 %를 잰다.
     * 기본값은 클라이언트 영역 2560x1494 게임 창에서 잰 값이다.
     */
    VITALS(
        "체력/마력", Rectangle2D.Double(0.8578, 0.8246, 0.1211, 0.0576), Reader.BARS,
        pickHint = "체력/마력 막대 두 줄만 꼭 맞게 드래그하세요. 점선은 지금 쓰는 영역이에요.",
    ),

    /**
     * 장비창 가운데에 서 있는 내 캐릭터 그림. 여기 있는 색으로 맵에서 캐릭터를 찾는다. 모니터 화면에는 보이지 않는다.
     * 기본값은 클라이언트 영역 2554x1491 게임 창에서 잰 값이다.
     */
    PORTRAIT(
        "내 캐릭터 (장비창)", Rectangle2D.Double(0.8340, 0.2998, 0.0509, 0.1006), Reader.CHARACTER,
        pickHint = "장비창 가운데 내 캐릭터를 감싸게 드래그하세요. 옆 장비 칸은 빼 주세요.",
    ),

    /** 캐릭터가 돌아다니는 맵 화면 전체 (테두리와 채팅창 제외). 기본값은 [PORTRAIT]와 같은 창에서 잰 값이다 */
    FIELD(
        "내 캐릭터", Rectangle2D.Double(0.0274, 0.0423, 0.7068, 0.7257), Reader.CHARACTER,
        pickHint = "맵이 보이는 곳 전체를 드래그하세요. 테두리와 채팅창은 빼 주세요.",
    ),

    /** 우하단 맨 아래 좌표 줄 (예: 0039 0144). 기본값은 [PORTRAIT]와 같은 창에서 잰 값이다 */
    COORDS(
        "좌표", Rectangle2D.Double(0.8614, 0.9310, 0.1135, 0.0349), Reader.CHARACTER,
        pickHint = "우하단 맨 아래 좌표 숫자 줄만 드래그하세요.",
    );

    enum class Reader {
        /** OCR 서버로 글자를 읽는다 */
        OCR,
        /** 앱에서 체력/마력 막대 색으로 잰다 */
        BARS,
        /** 앱에서 내 캐릭터 위치, 주변 몬스터, 좌표를 읽는다 ([PORTRAIT], [FIELD], [COORDS]를 같이 쓴다) */
        CHARACTER,
    }
}
