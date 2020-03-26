package com.zen.alchan.data.datasource

import com.apollographql.apollo.api.Response
import io.reactivex.Observable

interface MediaDataSource {
    fun getGenre(): Observable<Response<GenreQuery.Data>>
}