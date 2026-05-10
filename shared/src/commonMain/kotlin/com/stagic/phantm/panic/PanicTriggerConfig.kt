package com.stagic.phantm.panic

enum class TriggerType {
    NONE,
    WRONG_PIN_COUNT,
    VOLUME_COMBO,
}

data class PanicTriggerConfig(
    val type: TriggerType = TriggerType.NONE,
    val wrongPinCountThreshold: Int = 5,
    val pin: String? = null,
)
