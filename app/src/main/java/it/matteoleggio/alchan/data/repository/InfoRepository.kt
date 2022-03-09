package it.matteoleggio.alchan.data.repository

import androidx.lifecycle.LiveData
import it.matteoleggio.alchan.data.network.Resource
import it.matteoleggio.alchan.data.response.Announcement
import it.matteoleggio.alchan.data.response.SpotifySearch
import it.matteoleggio.alchan.data.response.YouTubeSearch

interface InfoRepository {
    val announcementResponse: LiveData<Resource<Announcement>>
    val lastAnnouncementId: Int?
    val youTubeVideoResponse: LiveData<Resource<YouTubeSearch>>
    val spotifyTrackResponse: LiveData<Resource<SpotifySearch>>

    fun getAnnouncement()
    fun setLastAnnouncementId(value: Int)
    fun getYouTubeVideo(query: String)
    fun getSpotifyTrack(query: String)
}