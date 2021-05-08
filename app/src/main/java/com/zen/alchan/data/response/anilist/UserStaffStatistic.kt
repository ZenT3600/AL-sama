package com.zen.alchan.data.response.anilist

data class UserStaffStatistic(
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val minutesWatched: Int = 0,
    val chaptersRead: Int = 0,
    val mediaIds: List<Int> = listOf(),
    val staff: Staff? = null
)