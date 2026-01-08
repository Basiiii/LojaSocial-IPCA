package com.lojasocial.app.domain.campaign

import java.util.Date

data class Campaign(
    val id: String = "",
    val name: String = "",
    val startDate: Date = Date(),
    val endDate: Date = Date()
)
