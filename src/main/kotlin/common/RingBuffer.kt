package common

class RingBuffer<T>(val size: Int, init: (index: Int) -> T) {
    private val list = MutableList(size, init)

    private var index = 0

    fun get(index: Int): T = list[index]

    fun append(element: T) = list.set(index++ % size, element)

    fun toList() = list.toList()
}