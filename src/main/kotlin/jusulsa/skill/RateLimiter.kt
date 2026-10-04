package jusulsa.skill

import java.util.ArrayDeque

/**
 * 직전 [windowMillis] 안에 [limit]번까지만 허락한다.
 * 서버의 1초 경계를 알 수 없어서, 어느 1초 구간을 잘라도 limit을 넘지 않는 슬라이딩 윈도우로 센다.
 * 키가 서버에 닿는 시간이 매번 조금씩 달라서 1초에 여유를 조금 더 둔다. 정확히 1초 뒤에 쓰면 같은 초로 잡혀 무시될 수 있다.
 */
class RateLimiter(
    private val limit: Int,
    private val windowMillis: Long = 1_100L,
) {
    private val history = ArrayDeque<Long>()

    /** 지금 하나 더 써도 되면 0, 아니면 기다려야 할 시간(ms) */
    @Synchronized
    fun waitMillis(now: Long): Long {
        while (history.isNotEmpty() && now - history.peekFirst() >= windowMillis) history.pollFirst()
        return if (history.size < limit) 0 else history.peekFirst() + windowMillis - now
    }

    /** 지금 바로 몇 번 더 쓸 수 있는지 */
    @Synchronized
    fun remaining(now: Long): Int {
        while (history.isNotEmpty() && now - history.peekFirst() >= windowMillis) history.pollFirst()
        return (limit - history.size).coerceAtLeast(0)
    }

    @Synchronized
    fun record(now: Long) {
        history.addLast(now)
    }
}
