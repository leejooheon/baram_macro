package common.base

import common.UiStateHolder
import common.model.MoveEvent
import common.model.PointModel
import common.robot.DisplayProvider
import common.model.UiEvent
import common.model.UiState
import common.model.UiState.Type
import common.network.OcrClient
import common.network.createHttpClient
import common.util.onError
import common.util.onSuccess
import follower.macro.FollowerMacro
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import kotlin.time.Duration

abstract class BaseViewModel {
    val ocrClient = OcrClient(createHttpClient())

    abstract fun dispatch(event: UiEvent): Job

    suspend inline fun updateCoordinates(
        duration: Duration,
        crossinline action: suspend (Pair<String, String>) -> Unit,
    ) {
        val startTime = System.currentTimeMillis()
        val xScreen = DisplayProvider.capture(Type.X)
        val yScreen = DisplayProvider.capture(Type.Y)
        ocrClient.readImage(xScreen).onSuccess {
            val x = it.results.firstOrNull() ?: return
            ocrClient.readImage(yScreen).onSuccess {
                val y = it.results.firstOrNull() ?: return
                val endTime = System.currentTimeMillis() - startTime
                UiStateHolder.updateCoordinates(
                    x = x,
                    y = y,
                    xScreen =xScreen,
                    yScreen = yScreen,
                    time = endTime
                )

                action.invoke(x to y)
                delay(duration)
            }
        }
    }
    suspend inline fun updateFromRemote(
        type: Type,
        duration: Duration,
        crossinline action: suspend (UiState) -> Unit,
    ) = withContext(Dispatchers.IO) {
        while (isActive) {
            val screen = DisplayProvider.capture(type)

            ocrClient
                .readImage(screen)
                .onSuccess {
                    val state = UiStateHolder.update(
                        type = type,
                        state = getStateFromType(type).copy(
                            texts = it.results,
                            image = screen
                        )
                    )
                    action.invoke(state)
                }
                .onError {
                    updateImage(
                        type = type,
                        image = screen,
                        texts = emptyList()
                    )
                }
            delay(duration)
        }
    }

    protected suspend fun updateFromLocal(
        type: Type,
        duration: Duration,
    ) = withContext(Dispatchers.IO) {

        while (isActive) {
            val screen = DisplayProvider.capture(type)
            val text = TextDetecter.detectString(screen)

            updateImage(
                type = type,
                image = screen,
                texts = listOf(text)
            )

            delay(duration)
        }
    }
    fun getStateFromType(type: Type): UiState.CommonState {
        val state = UiStateHolder.state.value
        return when(type) {
            Type.X -> state.xState
            Type.Y -> state.yState
            Type.BUFF -> state.buffState
            Type.MAGIC_RESULT -> state.magicResultState
        }
    }

    suspend fun updateImage(
        image: BufferedImage,
        texts: List<String>,
        type: Type
    ) {
        val state = getStateFromType(type).copy(
            image = image,
            texts = texts
        )
        UiStateHolder.update(
            type = type,
            state = state
        )
    }
}