package ocr.model

/** 맵에서의 네 방향. [dx]/[dy]는 한 칸 옮길 때 화면 좌표 변화 (오른쪽/아래가 +) */
enum class Direction(val dx: Int, val dy: Int, val arrow: String) {
    UP(0, -1, "↑"),
    DOWN(0, 1, "↓"),
    LEFT(-1, 0, "←"),
    RIGHT(1, 0, "→"),
}
