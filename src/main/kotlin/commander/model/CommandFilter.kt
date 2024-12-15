package commander.model

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent

val macroCommandFilter = listOf(
    NativeKeyEvent.VC_ESCAPE,
    NativeKeyEvent.VC_BACKQUOTE,
    NativeKeyEvent.VC_F1,
    NativeKeyEvent.VC_F2,
    NativeKeyEvent.VC_F3,
    NativeKeyEvent.VC_F4,
    NativeKeyEvent.VC_F5,
)

const val ctrlCommand = NativeKeyEvent.VC_CONTROL
val ctrlCommandFilter = listOf(
    NativeKeyEvent.VC_W,
    NativeKeyEvent.VC_A,
    NativeKeyEvent.VC_S,
    NativeKeyEvent.VC_D,
)

fun Int.commandToString(): String {
    return when(this) {
        NativeKeyEvent.VC_ESCAPE -> "VC_ESCAPE"
        NativeKeyEvent.VC_BACKQUOTE -> "VC_BACKQUOTE"
        NativeKeyEvent.VC_F1 -> "VC_F1"
        NativeKeyEvent.VC_F2 -> "VC_F2"
        NativeKeyEvent.VC_F3 -> "VC_F3"
        NativeKeyEvent.VC_F4 -> "VC_F4"
        NativeKeyEvent.VC_F5 -> "VC_F5"
        NativeKeyEvent.VC_W -> "VC_W"
        NativeKeyEvent.VC_A -> "VC_A"
        NativeKeyEvent.VC_S -> "VC_S"
        NativeKeyEvent.VC_D -> "VC_D"
        else -> "Unknown Key"
    }
}