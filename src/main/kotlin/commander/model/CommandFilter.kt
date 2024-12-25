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

const val ctrlCommand = NativeKeyEvent.VC_SHIFT
val ctrlCommandFilter = listOf(
    NativeKeyEvent.VC_W,
    NativeKeyEvent.VC_A,
    NativeKeyEvent.VC_S,
    NativeKeyEvent.VC_D,
)

val moveCommandFilter = listOf(
    NativeKeyEvent.VC_UP,
    NativeKeyEvent.VC_LEFT,
    NativeKeyEvent.VC_DOWN,
    NativeKeyEvent.VC_RIGHT,
)

val oneHandFilter = listOf(
    NativeKeyEvent.VC_PAGE_UP,
    NativeKeyEvent.VC_PAGE_DOWN,
    NativeKeyEvent.VC_KANJI,
)