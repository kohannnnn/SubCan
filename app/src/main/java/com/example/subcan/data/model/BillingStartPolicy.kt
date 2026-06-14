package com.example.subcan.data.model

enum class BillingStartPolicy(val label: String, val description: String) {
    SIGNUP_DATE("契約日から", "契約した日から課金期間が開始されます"),
    FIRST_OF_MONTH("月初から（日割りなし）", "毎月1日に課金期間が開始。日割りなし"),
    FIRST_OF_MONTH_PRORATED("月初から（日割りあり）", "毎月1日に課金期間が開始。初月は日割り計算")
}
