package com.zen.alchan.ui.home


import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.zen.alchan.R
import com.zen.alchan.helper.enums.BrowsePage
import com.zen.alchan.helper.enums.ResponseStatus
import com.zen.alchan.helper.libs.GlideApp
import com.zen.alchan.helper.utils.DialogUtility
import com.zen.alchan.ui.browse.BrowseActivity
import com.zen.alchan.ui.browse.media.overview.OverviewGenreRvAdapter
import com.zen.alchan.ui.explore.ExploreActivity
import com.zen.alchan.ui.search.SearchActivity
import com.zen.alchan.ui.seasonal.SeasonalActivity
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_loading.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import type.MediaType
import kotlin.math.abs

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {

    private val viewModel by viewModel<HomeViewModel>()

    private lateinit var releasingTodayAdapter: ReleasingTodayRvAdapter
    private lateinit var trendingAnimeAdapter: TrendingMediaRvAdapter
    private lateinit var trendingMangaAdapter: TrendingMediaRvAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        releasingTodayAdapter = assignReleasingTodayRvAdapter()
        releasingTodayRecyclerView.adapter = releasingTodayAdapter

        trendingAnimeAdapter = assignTrendingRvAdapter(viewModel.trendingAnimeList, MediaType.ANIME)
        trendingAnimeListRecyclerView.adapter = trendingAnimeAdapter

        trendingMangaAdapter = assignTrendingRvAdapter(viewModel.trendingMangaList, MediaType.MANGA)
        trendingMangaListRecyclerView.adapter = trendingMangaAdapter

        setupObserver()
        initLayout()
    }

    private fun setupObserver() {
        viewModel.viewerData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                initLayout()
            }
        })

        viewModel.viewerDataResponse.observe(viewLifecycleOwner, Observer {
            when (it.responseStatus) {
                ResponseStatus.LOADING -> loadingLayout.visibility = View.VISIBLE
                ResponseStatus.SUCCESS -> loadingLayout.visibility = View.GONE
                ResponseStatus.ERROR -> {
                    loadingLayout.visibility = View.GONE
                    DialogUtility.showToast(activity, it.message)
                }
            }
        })

        viewModel.trendingAnimeData.observe(viewLifecycleOwner, Observer {
            when (it.responseStatus) {
                ResponseStatus.LOADING -> trendingAnimeLoading.visibility = View.VISIBLE
                ResponseStatus.SUCCESS -> {
                    trendingAnimeLoading.visibility = View.GONE
                    if (it.data?.page?.media?.isNullOrEmpty() == false) {
                        initTrendingAnimeLayout(0)
                        viewModel.trendingAnimeList.clear()
                        it.data.page?.media?.forEachIndexed { index, media ->
                            viewModel.trendingAnimeList.add(TrendingMediaItem(media, index == 0))
                        }
                        trendingAnimeListRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
                ResponseStatus.ERROR -> {
                    trendingAnimeLoading.visibility = View.GONE
                    DialogUtility.showToast(activity, it.message)
                }
            }
        })

        viewModel.trendingMangaData.observe(viewLifecycleOwner, Observer {
            when (it.responseStatus) {
                ResponseStatus.LOADING -> trendingMangaLoading.visibility = View.VISIBLE
                ResponseStatus.SUCCESS -> {
                    trendingMangaLoading.visibility = View.GONE
                    if (it.data?.page?.media?.isNullOrEmpty() == false) {
                        initTrendingMangaLayout(0)
                        viewModel.trendingMangaList.clear()
                        it.data.page?.media?.forEachIndexed { index, media ->
                            viewModel.trendingMangaList.add(TrendingMediaItem(media, index == 0))
                        }
                        trendingMangaListRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
                ResponseStatus.ERROR -> {
                    trendingMangaLoading.visibility = View.GONE
                    DialogUtility.showToast(activity, it.message)
                }
            }
        })

        viewModel.releasingTodayData.observe(viewLifecycleOwner, Observer {
            if (it.responseStatus == ResponseStatus.SUCCESS) {
                if (!viewModel.hasNextPage) {
                    return@Observer
                }

                viewModel.hasNextPage = it.data?.page?.pageInfo?.hasNextPage ?: false
                viewModel.page += 1
                viewModel.isInit = true

                it.data?.page?.media?.forEach { media ->
                    var currentEpisode = 0
                    if (media?.nextAiringEpisode != null) {
                        if (media.nextAiringEpisode.timeUntilAiring < 3600 * 24) {
                            viewModel.releasingTodayList.add(ReleasingTodayItem(media, media.nextAiringEpisode.timeUntilAiring))
                        } else {
                            currentEpisode = media.nextAiringEpisode.episode - 1
                        }
                    }

                    if (media?.airingSchedule != null && media?.airingSchedule.edges?.isNullOrEmpty() == false) {
                        val currentEpisodeSchedule = media.airingSchedule.edges.find { edge -> edge?.node?.episode == currentEpisode }
                        if (currentEpisodeSchedule != null && abs(currentEpisodeSchedule.node?.timeUntilAiring!!) < 3600 * 24) {
                            viewModel.releasingTodayList.add(ReleasingTodayItem(media, currentEpisodeSchedule.node.timeUntilAiring))
                        }
                    }
                }

                if (viewModel.hasNextPage) {
                    viewModel.getReleasingToday()
                } else {
                    releasingTodayAdapter = assignReleasingTodayRvAdapter()
                    releasingTodayRecyclerView.adapter = releasingTodayAdapter
                    releasingTodayRecyclerView.visibility = if (viewModel.releasingTodayList.isNullOrEmpty()) View.GONE else View.VISIBLE
                    noNewEpisodeText.visibility = if (viewModel.releasingTodayList.isNullOrEmpty()) View.VISIBLE else View.GONE
                }
            }
        })

        viewModel.initData()
        if (!viewModel.isInit) {
            viewModel.getReleasingToday()
        }
    }

    private fun initLayout() {
        homeRefreshLayout.setOnRefreshListener {
            homeRefreshLayout.isRefreshing = false
            viewModel.initData()
        }

        val user = viewModel.viewerData.value

        GlideApp.with(this).load(R.drawable.welcome_background).into(headerImage)
        searchBar.setOnClickListener { startActivity(Intent(activity, SearchActivity::class.java)) }

        greetingsText.text = "Hello, ${user?.name}."
        GlideApp.with(this).load(user?.avatar?.large).apply(RequestOptions.circleCropTransform()).into(userAvatar)

        exploreMenu.setOnClickListener {
            MaterialAlertDialogBuilder(activity)
                .setItems(viewModel.explorePageArray) { _, which ->
                    val intent = Intent(activity, ExploreActivity::class.java)
                    intent.putExtra(ExploreActivity.EXPLORE_PAGE, viewModel.explorePageArray[which])
                    startActivity(intent)
                }
                .show()
        }

        seasonalChartMenu.setOnClickListener {
            startActivity(Intent(activity, SeasonalActivity::class.java))
        }
    }

    private fun assignTrendingRvAdapter(list: List<TrendingMediaItem>, mediaType: MediaType): TrendingMediaRvAdapter {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val width = metrics.widthPixels / 4

        return TrendingMediaRvAdapter(activity!!, list, mediaType, width, object : TrendingMediaRvAdapter.TrendingMediaListener {
            override fun passSelectedMedia(position: Int, mediaType: MediaType) {
                if (mediaType == MediaType.ANIME) {
                    initTrendingAnimeLayout(position)
                    viewModel.trendingAnimeList.forEachIndexed { index, it ->
                        it.isSelected = index == position
                    }
                    trendingAnimeListRecyclerView.adapter?.notifyDataSetChanged()
                } else if (mediaType == MediaType.MANGA) {
                    initTrendingMangaLayout(position)
                    viewModel.trendingMangaList.forEachIndexed { index, it ->
                        it.isSelected = index == position
                    }
                    trendingMangaListRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
        })
    }

    private fun initTrendingAnimeLayout(highlightIndex: Int) {
        trendingAnimeCard.visibility = View.VISIBLE
        trendingAnimeListRecyclerView.visibility = View.VISIBLE

        val highlightAnime = viewModel.trendingAnimeData.value?.data?.page?.media!![highlightIndex]
        GlideApp.with(this).load(highlightAnime?.bannerImage).into(trendingAnimeBanner)
        GlideApp.with(this).load(highlightAnime?.coverImage?.large).into(trendingAnimeCoverImage)
        trendingAnimeTitleText.text = highlightAnime?.title?.userPreferred
        if (highlightAnime?.studios?.edges?.isNullOrEmpty() == false) {
            trendingAnimeCreatorText.text = highlightAnime.studios.edges[0]?.node?.name
        } else {
            trendingAnimeCreatorText.text = ""
        }
        trendingAnimeScore.text = highlightAnime?.averageScore?.toString() ?: "0"
        trendingAnimeFavorite.text = highlightAnime?.favourites?.toString() ?: "0"
        trendingAnimeDescriptionText.text = HtmlCompat.fromHtml(highlightAnime?.description ?: getString(R.string.no_description), HtmlCompat.FROM_HTML_MODE_LEGACY)

        if (!highlightAnime?.genres.isNullOrEmpty()) {
            trendingAnimeGenreRecyclerView.visibility = View.VISIBLE
            trendingAnimeGenreRecyclerView.adapter = OverviewGenreRvAdapter(highlightAnime?.genres!!, object : OverviewGenreRvAdapter.OverviewGenreListener {
                override fun passSelectedGenre(genre: String) { }
            })
        } else {
            trendingAnimeGenreRecyclerView.visibility = View.GONE
        }

        trendingAnimeCard.setOnClickListener {
            val intent = Intent(activity, BrowseActivity::class.java)
            intent.putExtra(BrowseActivity.TARGET_PAGE, BrowsePage.ANIME.name)
            intent.putExtra(BrowseActivity.LOAD_ID, highlightAnime?.id)
            startActivity(intent)
        }
    }

    private fun initTrendingMangaLayout(highlightIndex: Int) {
        trendingMangaCard.visibility = View.VISIBLE
        trendingMangaListRecyclerView.visibility = View.VISIBLE

        val highlightManga = viewModel.trendingMangaData.value?.data?.page?.media!![highlightIndex]
        GlideApp.with(this).load(highlightManga?.bannerImage).into(trendingMangaBanner)
        GlideApp.with(this).load(highlightManga?.coverImage?.large).into(trendingMangaCoverImage)
        trendingMangaTitleText.text = highlightManga?.title?.userPreferred
        var creatorList = ""
        highlightManga?.staff?.edges?.forEachIndexed { index, it ->
            creatorList += it?.node?.name?.full
            if (index != highlightManga.staff.edges.lastIndex) creatorList += ", "
        }
        trendingMangaCreatorText.text = creatorList
        trendingMangaScore.text = highlightManga?.averageScore?.toString() ?: "0"
        trendingMangaFavorite.text = highlightManga?.favourites?.toString() ?: "0"
        trendingMangaDescriptionText.text = HtmlCompat.fromHtml(highlightManga?.description ?: getString(R.string.no_description), HtmlCompat.FROM_HTML_MODE_LEGACY)

        if (!highlightManga?.genres.isNullOrEmpty()) {
            trendingMangaGenreRecyclerView.visibility = View.VISIBLE
            trendingMangaGenreRecyclerView.adapter = OverviewGenreRvAdapter(highlightManga?.genres!!, object : OverviewGenreRvAdapter.OverviewGenreListener {
                override fun passSelectedGenre(genre: String) { }
            })
        } else {
            trendingMangaGenreRecyclerView.visibility = View.GONE
        }

        trendingMangaCard.setOnClickListener {
            val intent = Intent(activity, BrowseActivity::class.java)
            intent.putExtra(BrowseActivity.TARGET_PAGE, BrowsePage.MANGA.name)
            intent.putExtra(BrowseActivity.LOAD_ID, highlightManga?.id)
            startActivity(intent)
        }
    }

    private fun assignReleasingTodayRvAdapter(): ReleasingTodayRvAdapter {
        return ReleasingTodayRvAdapter(activity!!, viewModel.releasingTodayList, object : ReleasingTodayRvAdapter.ReleasingTodayListener {
            override fun passSelectedAnime(mediaId: Int) {
                val intent = Intent(activity, BrowseActivity::class.java)
                intent.putExtra(BrowseActivity.TARGET_PAGE, BrowsePage.ANIME.name)
                intent.putExtra(BrowseActivity.LOAD_ID, mediaId)
                startActivity(intent)
            }
        })
    }

    class ReleasingTodayItem(val releasingToday: ReleasingTodayQuery.Medium?, val timestamp: Int)

    class TrendingMediaItem(val trendingMedia: TrendingMediaQuery.Medium?, var isSelected: Boolean)
}
