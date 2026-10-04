package jusulsa.skill

import java.util.ArrayDeque

/**
 * 직전 [windowMillis] 안에 [limit]번까지만 허락한다.
 * 서버의 1초 경계를 알 수 없어서, 어느 1초 구간을 잘라도 limit을 넘지 않는 슬라이딩 윈도우로 센다.
 */
class RateLimiter(
    private val limit: Int,
    private val windowMillis: Long = 1_000L,
) {
    private val history = ArrayDeque<Long>()

    /** 지금 하나 더 써도 되면 0, 아니면 기다려야 할 시간(ms) */
    @Synchronized
    fun waitMillis(now: Long): Long {
        while (history.isNotEmpty() && now - history.peekFirst() >= windowMillis) history.pollFirst()
        return if (history.size < limit) 0 else history.peekFirst() + windowMillis - now
    }

    @Synchronized
    fun record(now: Long) {
        history.addLast(now)
    }
}
