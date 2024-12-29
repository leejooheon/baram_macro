package follower.model

enum class MagicResultState(
    val tag: String,
) {
    NONE("NONE"),
    ME_DEAD("귀신"),
    OTHER_DEAD("다른"),
    NO_MP("마력"),
    ALREADY("이미"),
    GONGJEUNG("공력"),
}