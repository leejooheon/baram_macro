package jusulsa.skill

import detector.Aim
import java.awt.event.KeyEvent

/**
 * 주술사 마법과 시전 규칙.
 * 숫자 칸(1~0)은 키 하나, 나머지는 shift+z 후 알파벳(대문자 칸은 shift를 누른 채로)으로 고른다.
 *
 * @param rateGroup 같은 그룹끼리 합쳐서 초당 횟수를 제한한다
 * @param minIntervalMillis 같은 마법을 다시 쓰기까지의 최소 간격
 */
enum class Skill(
    val key: Int,
    val alphabet: Boolean = false,
    val upper: Boolean = false,
    val rateGroup: RateGroup? = null,
    val minIntervalMillis: Long = 0,
) {
    HELLFIRE(KeyEvent.VK_1),           // a(1): 헬파이어
    GONGJEUNG(KeyEvent.VK_2),          // b(2): 공력증강
    CHUM1(KeyEvent.VK_3, minIntervalMillis = 100), // c(3): 극진뢰격참주'첨
    CHUM2(KeyEvent.VK_4, minIntervalMillis = 100), // d(4): 진뢰격참주'첨
    HONDON(KeyEvent.VK_5),             // e(5): 혼돈
    JUNGDOK(KeyEvent.VK_6),            // f(6): 중독
    JEOJU(KeyEvent.VK_7, rateGroup = RateGroup.CURSE),   // g(7): 저주
    SAMME(KeyEvent.VK_8),              // h(8): 삼매진화
    HEAL(KeyEvent.VK_9, rateGroup = RateGroup.HEAL), // i(9): 태양의기원
    JIPOK(KeyEvent.VK_0),              // j(0): 지폭지술
    BOHO(KeyEvent.VK_M, alphabet = true),    // m: 보호
    MUJANG(KeyEvent.VK_N, alphabet = true),  // n: 무장
    MAGII(KeyEvent.VK_O, alphabet = true),   // o: 마기지체
    // 5매각용. 게임 마법창 칸이 다르면 여기만 바꾼다
    MABEE(KeyEvent.VK_P, alphabet = true),   // p: 마비
    JULMANG(KeyEvent.VK_Q, alphabet = true, rateGroup = RateGroup.CURSE), // q: 절망
    HWALRYEOK(KeyEvent.VK_R, alphabet = true), // r: 활력
}

/** 서버 기준 1초에 쓸 수 있는 횟수를 같이 세는 마법 묶음 */
enum class RateGroup(val limit: Int) {
    HEAL(3),
    /** 저주 + 절망 */
    CURSE(8),
}

/** 마법을 고른 뒤 대상을 잡는 방법 */
sealed interface Target {
    /** 키만 누른다. 숫자 칸은 직전 대상에게, 즉시 시전 마법은 바로 나간다 */
    data object Current : Target
    /** Enter로 지금 커서 대상에게 */
    data object Confirm : Target
    /** HOME으로 나를 잡고 Enter */
    data object Me : Target
    /**
     * 방향키로 대상을 잡고 Enter. fromMe면 HOME으로 나를 잡은 뒤 방향키를 누른다.
     * 방향으로 대상을 잡으므로 마법과 상관없이 사용자 이동키에 영향을 받는다.
     */
    data class Direction(val direction: Int, val fromMe: Boolean = false) : Target
    /** 마법을 고른 뒤 게임 창의 [aim] 지점(몹)을 마우스로 클릭한다. 방향키처럼 다른 몹으로 새지 않는다 */
    data class Click(val aim: Aim) : Target
}
