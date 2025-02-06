package jusulsa

import common.model.PositionResult
import common.robot.Keyboard
import common.util.Result
import common.util.onError
import common.util.onSuccess
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.seconds

class KingHelper(
    scope: CoroutineScope
) {
    enum class State(val tag: String) {
        DEFAULT(""),
        FIND_KING(""),
        MISSION_INTRODUCE_ONE("직접"),
        MISSION_INTRODUCE_TWO("받으려고"),
        MISSION_ANSWER_ONE("받켓습니다"),
        MISSION_ANSWER_TWO("받으시려오"),
        MISSION_ACCEPT("어명이오"),

        MISSION_CANCEL_REQUEST("이놈"),
        MISSION_CANCEL_ANSWER_ONE("취소시켜"),
        MISSION_CANCEL_ANSWER_TWO("취소시켜"),
        MISSION_CANCEL_ACCEPT("취소해달"),
        MISSION_CANCEL_FINISH("형벌로"),
    }

//    enum class State(val tags: List<String>) {
//        DEFAULT(emptyList()),
//        MISSION_INTRODUCE_ONE(listOf("직접")),
//        MISSION_ACCEPT(listOf("동달귀신", "독출")),
//        MISSION_FINISH(emptyList()),
//
//        MISSION_CANCEL_REQUEST(listOf("이놈")),
//    }

    private val _currentState = MutableSharedFlow<State>()
    val currentState = _currentState.asSharedFlow()

    init {
        scope.launch(Dispatchers.IO) {
            currentState.collectLatest { state ->
                when (state) {
                    State.DEFAULT -> { /* nothing */ }
                    State.FIND_KING -> {
                        while (!findKing()) delay(0.5.seconds)
                        _currentState.emit(State.MISSION_INTRODUCE_ONE)
                    }
                    else -> {
                        delay(0.5.seconds)
                        handleMissionState(state)
                    }
                }
            }
        }
    }

    suspend fun start() {
        println("start")
        _currentState.emit(State.FIND_KING)
    }
    suspend fun cancel() {
        println("cancel")
        _currentState.emit(State.DEFAULT)
    }

    private suspend fun findKing(): Boolean = withContext(Dispatchers.IO) {
        return@withContext when(val result = TextDetecter.findKing()) {
            is Result.Success -> {
                val model = result.data.result
                if(model.confidence > 0.9f) {
                    Keyboard.mouseClick(model.position.x, model.position.y)
                    true
                } else false
            }
            is Result.Error -> {
                false
            }
        }
    }

    private suspend fun findString(
        state: State
    ): Pair<PositionResult, PositionResult>? = withContext(Dispatchers.IO) {
        return@withContext when(val result = TextDetecter.conversationWithKing()) {
            is Result.Success -> {
                val models = result.data.result
                val target = findResultOrNull(
                    targets = listOf(state.tag),
                    models = models
                )
                val button = when(state) {
                    State.MISSION_ANSWER_ONE,
                    State.MISSION_CANCEL_ANSWER_ONE -> {
                        models.first() // 예외처리임
                    }
                    State.MISSION_ANSWER_TWO,
                    State.MISSION_CANCEL_ANSWER_TWO -> {
                        findOkButton(models)
                    }
                    else -> findNextButton(models)
                }
                println("state: $state, ${state.tag}")
                println(models)
                println()
                println(target)
                println()
                println(button)
                if(target == null || button == null) null
                else target to button
            }
            is Result.Error -> {
                null
            }
        }
    }

    private suspend fun handleMissionState(state: State) {
        val result = findString(state)
        result?.let { (target, button) ->
            when (state) {
                State.MISSION_INTRODUCE_ONE -> {
                    Keyboard.mouseClick(button.position.x, button.position.y)
                    _currentState.emit(State.MISSION_INTRODUCE_TWO)
                }
                State.MISSION_INTRODUCE_TWO -> {
                    Keyboard.mouseClick(button.position.x, button.position.y)
                    _currentState.emit(State.MISSION_ANSWER_ONE)
                }
                State.MISSION_ANSWER_ONE -> {
                    Keyboard.mouseClick(target.position.x, target.position.y)
                    _currentState.emit(State.MISSION_ANSWER_TWO)
                }
                State.MISSION_ANSWER_TWO -> {
                    Keyboard.mouseClick(button.position.x, button.position.y)
                    _currentState.emit(State.MISSION_ACCEPT)
                }
                State.MISSION_ACCEPT -> {
                    if(target.text.contains(TARGET)) {
                        println("success!!")
                    } else {
                        Keyboard.mouseClick(button.position.x, button.position.y)
                        _currentState.emit(State.MISSION_CANCEL_REQUEST)
                    }
                }
                State.MISSION_CANCEL_REQUEST -> {
                    Keyboard.mouseClick(button.position.x, button.position.y)
                    _currentState.emit(State.MISSION_CANCEL_ANSWER_ONE)
                }
                State.MISSION_CANCEL_ANSWER_ONE -> {
                    Keyboard.mouseClick(target.position.x, target.position.y)
                    _currentState.emit(State.MISSION_CANCEL_ANSWER_TWO)
                }
                State.MISSION_CANCEL_ANSWER_TWO -> {
                    Keyboard.mouseClick(target.position.x, target.position.y)
                    _currentState.emit(State.MISSION_CANCEL_ACCEPT)
                }
                State.MISSION_CANCEL_ACCEPT -> {
                    Keyboard.mouseClick(button.position.x, button.position.y)
                    _currentState.emit(State.MISSION_CANCEL_FINISH)
                }
                State.MISSION_CANCEL_FINISH -> {
                    Keyboard.mouseClick(button.position.x, button.position.y)
                    _currentState.emit(State.FIND_KING)
                }

                else -> {}
            }
        } ?: run {
            println("error: $state")
            // 텍스트를 찾지 못했을 경우 처리 (예: 재시도, 에러 처리 등)
            // 예시: _currentState.emit(state) // 현재 상태를 다시 emit 하여 재시도
        }
    }


    companion object {
        private val TARGET = "처녀" // 처녀귀신

        fun findNextButton(models: List<PositionResult>): PositionResult? {
            val targets = listOf("NE", "NS")
            return findResultOrNull(targets, models)
        }

        fun findOkButton(models: List<PositionResult>): PositionResult? {
            val targets = listOf("UK", "OK")
            return findResultOrNull(targets, models)
        }

        private fun findResultOrNull(
            targets: List<String>,
            models: List<PositionResult>
        ): PositionResult? {
            if(targets.isEmpty()) return null
            return models.firstOrNull { model ->
                targets.any { target -> model.text.contains(target, ignoreCase = true) }
            }
        }
    }
}