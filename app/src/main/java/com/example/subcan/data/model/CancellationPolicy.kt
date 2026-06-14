package com.example.subcan.data.model

enum class CancellationPolicy(val label: String, val description: String) {
    IMMEDIATE("即時停止", "解約すると即時に利用できなくなります"),
    END_OF_PERIOD("期間終了まで利用可", "解約しても課金期間終了まで利用できます"),
    REFUNDABLE("返金あり", "解約時に未使用期間分の返金があります")
}
