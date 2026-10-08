package jusulsa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.robot.Keyboard
import jusulsa.model.JusulsaUiState
import jusulsa.skill.Skill
import java.awt.event.KeyEvent

private val TestText = TextStyle(fontSize = 12.sp)

/**
 * 기능을 하나씩 눌러 보는 테스트 탭.
 * 버튼을 누르면 게임 창을 앞으로 가져온 뒤 그 기능만 실행한다. 정지 버튼이나 ESC, PageDown으로 멈춘다.
 */
@Composable
internal fun JusulsaTestTab(
    uiState: JusulsaUiState,
    onTest: (String, suspend MacroDetailAction2.() -> Unit) -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(uiState.testStatus.ifEmpty { "버튼을 누르면 게임 창으로 넘어가서 실행합니다" }, style = TestText)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828), contentColor = Color.White),
            ) { Text("정지", style = TestText) }
        }

        Section("첨 (극진뢰·진뢰)")
        TestButton("첨 켜기: 3·4 누른 채 넘버락") { onTest("첨 켜기") { startAutoChum() } }
        TestButton("넘버락 한 번") { onTest("넘버락") { Keyboard.pressAndRelease(KeyEvent.VK_NUM_LOCK) } }

        Section("저주 (7 누르고 있기)")
        TestButton("저주") { onTest("저주") { holdCast(Skill.JEOJU) } }

        Section("중독 (6 + 방향키 + 엔터 누르고 있기)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DIRECTIONS.forEach { (label, key) ->
                TestButton(label, Modifier.weight(1f)) { onTest("중독 $label") { holdCast(Skill.JUNGDOK, key) } }
            }
        }

        Section("전체")
        TestButton("첨첨 전체 (PageUp과 같음)") { onTest("첨첨 전체") { chumChum() } }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = TestText, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun TestButton(label: String, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) { Text(label, style = TestText) }
}

private val DIRECTIONS = listOf(
    "↑" to KeyEvent.VK_UP,
    "←" to KeyEvent.VK_LEFT,
    "↓" to KeyEvent.VK_DOWN,
    "→" to KeyEvent.VK_RIGHT,
)
