package common.base

import common.display.DisplayProvider
import common.event.UiEvent
import common.model.Type
import common.model.UiState
import common.network.OcrClient
import common.network.createHttpClient
import common.robot.Keyboard
import common.util.onError
import common.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.image.BufferedImage
import kotlin.time.Duration

abstract class BaseViewModel {
    private val updateMutex = Mutex()
    private val ocrClient = OcrClient(createHttpClient())

    abstract fun dispatch(event: UiEvent): Job

    protected suspend fun observeAndUpdate(type: Type, duration: Duration) {
        captureScreen(
            type = type,
            duration = duration
        )
            .flatMapConcat { screen ->
                readText(screen)
                    .map { result -> screen to result }
            }
            .distinctUntilChanged()
            .collect { (screen, result) ->
                updateImage(
                    image = screen,
                    texts = result,
                    type = type
                )
            }
    }

    private fun captureScreen(
        type: Type,
        duration: Duration,
    ) = flow {
        val displayProvider = DisplayProvider(Keyboard.robot)
        while (true) {
            val state = getStateFromType(type)
            val screen = displayProvider.capture(state.rectangle)
            emit(screen)
            delay(duration)
        }
    }.flowOn(Dispatchers.IO)

    private fun readText(image: BufferedImage) = flow {
        val isRunning = UiStateHolder.state.value.isRunning
        if(isRunning) {
            ocrClient
                .readImage(image)
                .onSuccess { model ->
                    emit(model.results)
                }
                .onError {
                    emit(listOf("error"))
                }
        } else {
            emit(listOf("-"))
        }
    }.flowOn(Dispatchers.IO)

    private fun getStateFromType(type: Type): UiState.CommonState {
        val state = UiStateHolder.state.value
        return when(type) {
            Type.X -> state.xState
            Type.Y -> state.yState
            Type.BUFF -> state.buffState
            Type.MAGIC_RESULT -> state.magicResultState
        }
    }

    private suspend fun updateImage(
        image: BufferedImage,
        texts: List<String>,
        type: Type
    ) = updateMutex.withLock {
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