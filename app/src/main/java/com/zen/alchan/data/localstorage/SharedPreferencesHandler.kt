package com.zen.alchan.data.localstorage

import com.zen.alchan.data.entitiy.AppSetting
import com.zen.alchan.data.entitiy.MediaFilter
import com.zen.alchan.data.entitiy.ListStyle

interface SharedPreferencesHandler {
    var bearerToken: String?
    var guestLogin: Boolean?
    var animeListStyle: ListStyle?
    var mangaListStyle: ListStyle?
    var animeFilter: MediaFilter?
    var mangaFilter: MediaFilter?
    var appSetting: AppSetting?
    var followingCount: Int?
    var followersCount: Int?
}