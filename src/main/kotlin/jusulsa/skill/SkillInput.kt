package jusulsa.skill

import common.robot.Keyboard
import kotlinx.coroutines.delay
import java.awt.event.KeyEvent

/** 매크로와 UseCase가 같이 쓰는 마법 입력 */
object SkillInput {
    /** 마법을 고르고 HOME으로 나를 대상으로 잡은 뒤 action을 이어서 보낸다 */
    suspend inline fun focusMe(
        keyEvent: Int,
        crossinline action: suspend () -> Unit,
    ) = Keyboard.atomic {
        Keyboard.pressAndRelease(keyEvent)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        action.invoke()
    }

    /** shift+z 알파벳 마법을 건다. forMe면 HOME으로 나를 대상으로, enter면 Enter로 시전한다 */
    suspend fun castAlphabetMagic(magic: Int, forMe: Boolean, enter: Boolean) = Keyboard.atomic {
        selectAlphabetMagic(magic)

        if (forMe) {
            delay(SKILL_DELAY)
            Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        }

        if (enter) {
            delay(SKILL_DELAY)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        }
    }

    // shift+z 후 알파벳, 대문자 칸은 shift를 누른 채로 알파벳까지 입력
    suspend fun selectAlphabetMagic(magic: Int, upper: Boolean = false) = Keyboard.atomic {
        Keyboard.press(KeyEvent.VK_SHIFT)
        try {
            delay(SKILL_DELAY)
            Keyboard.pressAndRelease(KeyEvent.VK_Z)
            if (upper) {
                delay(SKILL_DELAY)
                Keyboard.pressAndRelease(magic)
            }
        } finally {
            Keyboard.release(KeyEvent.VK_SHIFT)
        }

        if (!upper) {
            delay(SKILL_DELAY)
            Keyboard.pressAndRelease(magic)
        }
    }
}
