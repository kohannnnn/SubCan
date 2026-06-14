package com.example.subcan.data.model

enum class BillingCycle(val label: String, val months: Int) {
    WEEKLY("週額", 0),
    MONTHLY("月額", 1),
    QUARTERLY("3ヶ月", 3),
    SEMI_ANNUAL("半年", 6),
    ANNUAL("年額", 12);

    /**
     * 月額換算の係数を返す。
     * 例: ANNUAL → price / 12, WEEKLY → price * 4.345
     */
    fun toMonthlyFactor(): Double = when (this) {
        WEEKLY -> 4.345

        // 平均週数/月
        MONTHLY -> 1.0

        QUARTERLY -> 1.0 / 3.0

        SEMI_ANNUAL -> 1.0 / 6.0

        ANNUAL -> 1.0 / 12.0
    }
}
