package it.matteoleggio.alchan.ui.browse.media.stats


import MediaOverviewQuery
import MediaStatsQuery
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.gson.Gson
import it.matteoleggio.alchan.R
import it.matteoleggio.alchan.data.response.AnimeStats
import it.matteoleggio.alchan.data.response.MangaStats
import it.matteoleggio.alchan.data.response.Scores
import it.matteoleggio.alchan.data.response.overview.MediaOverview
import it.matteoleggio.alchan.helper.Constant
import it.matteoleggio.alchan.helper.JikanApiHelper
import it.matteoleggio.alchan.helper.enums.ResponseStatus
import it.matteoleggio.alchan.helper.pojo.StatusDistributionItem
import it.matteoleggio.alchan.helper.utils.AndroidUtility
import it.matteoleggio.alchan.helper.utils.DialogUtility
import it.matteoleggio.alchan.ui.base.BaseFragment
import it.matteoleggio.alchan.ui.browse.media.MediaFragment
import it.matteoleggio.alchan.ui.common.ChartDialog
import it.matteoleggio.alchan.ui.settings.app.AppSettingsViewModel
import kotlinx.android.synthetic.main.fragment_media_stats.*
import kotlinx.android.synthetic.main.layout_loading.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import org.koin.androidx.viewmodel.ext.android.viewModel
import type.MediaType
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/**
 * A simple [Fragment] subclass.
 */
class MediaStatsFragment(val alId: Int, val mediaType: MediaType) : BaseFragment() {
    private val viewModel by viewModel<MediaStatsViewModel>()
    private val viewModelSettings by viewModel<AppSettingsViewModel>()

    var votes = arrayListOf<Int>()
    var failedMal = false
    var alData: MediaOverview? = null
    var animeStats: AnimeStats? = null
    var mangaStats: MangaStats? = null

    private var mediaData: MediaStatsQuery.Media? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_stats, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.mediaId = arguments?.getInt(MediaFragment.MEDIA_ID)

        setupObserver()
    }

    private fun setupObserver() {
        viewModel.mediaStatsData.observe(viewLifecycleOwner, Observer {
            when (it.responseStatus) {
                ResponseStatus.LOADING -> loadingLayout.visibility = View.VISIBLE
                ResponseStatus.SUCCESS -> {
                    loadingLayout.visibility = View.GONE

                    if (viewModel.mediaId != it.data?.media?.id) {
                        return@Observer
                    }

                    viewModel.mediaData = it.data?.media
                    mediaData = it.data?.media
                    initLayout()
                }
                ResponseStatus.ERROR -> {
                    loadingLayout.visibility = View.GONE
                    DialogUtility.showToast(activity, it.message)
                }
            }
        })

        if (viewModel.mediaData == null) {
            viewModel.getMediaStats()
        } else {
            mediaData = viewModel.mediaData
            initLayout()
        }
    }

    private fun initLayout() {
        handlePerformance()
        handleRankings()
        handleStatusDistribution()
        handleScoreDistribution()
    }

    private fun addScoreToArray(scores: Scores, n: Int) {
        for (i in 0..scores.votes!!) {
            this.votes.add(n)
        }
    }

    fun getMalId(url: String): MediaOverview {
        val httpClient = OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val jsonObjectChild = JSONObject()
        jsonObjectChild.put("id", alId)
        val jsonObject = JSONObject()
        jsonObject.put("query", MediaOverviewQuery.QUERY_DOCUMENT)
        jsonObject.put("variables", jsonObjectChild)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val formBody = jsonObject.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        println(httpClient.newCall(request).execute().body?.string().toString())
        return Gson().fromJson(
            httpClient.newCall(request).execute().body?.string(),
            MediaOverview().javaClass
        )
    }

    private fun handlePerformance() {
        mediaAvgScoreText.text = "${mediaData?.averageScore?.toString() ?: "0"}%"
        mediaMeanScoreText.text = "${mediaData?.meanScore?.toString() ?: "0"}%"
        mediaPopularityText.text = mediaData?.popularity?.toString() ?: "0"
        mediaFavoritesText.text = mediaData?.favourites?.toString() ?: "0"

        if (viewModelSettings.appSettings.fetchFromMal) {
            malPerformanceLayout.visibility = View.VISIBLE
            malPerformanceTextView.visibility = View.VISIBLE
            thread(start = true) {
                try {
                    alData = getMalId(Constant.ANILIST_API_URL)
                    if (alData?.data?.Media?.type == MediaType.ANIME.toString()) {
                        animeStats =
                            JikanApiHelper().getAnimeStats(alData?.data?.Media?.idMal!!)
                        println(animeStats!!.data?.scores?.size)
                        var i = 1
                        animeStats!!.data?.scores?.forEach {
                            this@MediaStatsFragment.addScoreToArray(it, i)
                            i++
                        }
                    } else {
                        mangaStats =
                            JikanApiHelper().getMangaStats(alData?.data?.Media?.idMal!!)
                        var i = 1
                        mangaStats!!.data?.scores?.forEach {
                            this@MediaStatsFragment.addScoreToArray(it, i)
                            i++
                        }
                    }
                } catch (e: java.lang.NullPointerException) { failedMal = true }
            }.join()
            if (!failedMal) {
                malMediaAvgScoreText.text = "${
                    votes.average().toString().substring(0, 3).toFloat().times(10).toInt()}%"
                if (alData?.data?.Media?.type == MediaType.ANIME.toString()) {
                    malTotalWatchesText.text = animeStats?.data?.total.toString()
                } else {
                    malTotalWatchesText.text = mangaStats?.data?.total.toString()
                }
            }
        }
    }

    private fun handleRankings() {
        if (mediaData?.rankings?.isNullOrEmpty() == true) {
            mediaStatsRankingLayout.visibility = View.GONE
            return
        }

        mediaStatsRankingLayout.visibility = View.VISIBLE
        mediaStatsRankingRecyclerView.adapter = MediaStatsRankingRvAdapter(mediaData?.rankings!!)
    }

    private fun handleStatusDistribution() {
        if (mediaData?.stats?.statusDistribution?.isNullOrEmpty() == true) {
            mediaStatsStatusLayout.visibility = View.GONE
            return
        }

        mediaStatsStatusLayout.visibility = View.VISIBLE

        val statusDistributionList = ArrayList<StatusDistributionItem>()

        val pieEntries = ArrayList<PieEntry>()
        mediaData?.stats?.statusDistribution?.forEach {
            val pieEntry = PieEntry(it?.amount!!.toFloat(), it.status?.toString())
            pieEntries.add(pieEntry)
            statusDistributionList.add(StatusDistributionItem(it.status?.name!!, it.amount, Constant.STATUS_COLOR_LIST[statusDistributionList.size]))
        }

        val pieDataSet = PieDataSet(pieEntries, "Score Distribution")
        pieDataSet.colors = Constant.STATUS_COLOR_LIST

        if (!viewModel.showStatsAutomatically && !failedMal) {
            mediaStatsStatusPieChart.visibility = View.GONE
            mediaStatsStatusShowButton.visibility = View.VISIBLE

            mediaStatsStatusShowButton.setOnClickListener {
                val dialog = ChartDialog()
                val bundle = Bundle()
                bundle.putString(ChartDialog.PIE_ENTRIES, viewModel.gson.toJson(pieDataSet))
                dialog.arguments = bundle
                dialog.show(childFragmentManager, null)
            }
        } else {
            try {
                val pieData = PieData(pieDataSet)
                pieData.setDrawValues(false)

                mediaStatsStatusPieChart.setHoleColor(ContextCompat.getColor(requireActivity(), android.R.color.transparent))
                mediaStatsStatusPieChart.setDrawEntryLabels(false)
                mediaStatsStatusPieChart.setTouchEnabled(false)
                mediaStatsStatusPieChart.description.isEnabled = false
                mediaStatsStatusPieChart.legend.isEnabled = false
                mediaStatsStatusPieChart.data = pieData
                mediaStatsStatusPieChart.invalidate()
            } catch (e: Exception) {
                DialogUtility.showToast(activity, e.localizedMessage)
            }

            mediaStatsStatusPieChart.visibility = View.VISIBLE
            mediaStatsStatusShowButton.visibility = View.GONE
        }

        mediaStatsStatusRecyclerView.adapter = MediaStatsStatusRvAdapter(requireActivity(), statusDistributionList)

        if (viewModelSettings.appSettings.fetchFromMal && !failedMal) {
            malMediaStatsStatusLayout.visibility = View.VISIBLE

            val malStatusDistributionList = ArrayList<StatusDistributionItem>()

            val malPieEntries = ArrayList<PieEntry>()

            if (mediaType == MediaType.ANIME) {
                malPieEntries.add(PieEntry(animeStats?.data?.watching!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "CURRENT",
                        animeStats?.data?.watching!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(animeStats?.data?.planToWatch!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "PLANNING",
                        animeStats?.data?.planToWatch!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(animeStats?.data?.completed!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "COMPLETED",
                        animeStats?.data?.completed!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(animeStats?.data?.dropped!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "DROPPED",
                        animeStats?.data?.dropped!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(animeStats?.data?.onHold!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "PAUSED",
                        animeStats?.data?.onHold!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
            } else {
                malPieEntries.add(PieEntry(mangaStats?.data?.reading!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "CURRENT",
                        mangaStats?.data?.reading!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(mangaStats?.data?.planToRead!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "PLANNING",
                        mangaStats?.data?.planToRead!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(mangaStats?.data?.completed!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "COMPLETED",
                        mangaStats?.data?.completed!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(mangaStats?.data?.dropped!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "DROPPED",
                        mangaStats?.data?.dropped!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
                malPieEntries.add(PieEntry(mangaStats?.data?.onHold!!.toFloat()))
                malStatusDistributionList.add(
                    StatusDistributionItem(
                        "PAUSED",
                        mangaStats?.data?.onHold!!,
                        Constant.STATUS_COLOR_LIST[malStatusDistributionList.size]
                    )
                )
            }

            val malPieDataSet = PieDataSet(malPieEntries, "Score Distribution")
            malPieDataSet.colors = Constant.STATUS_COLOR_LIST

            try {
                val malPieData = PieData(malPieDataSet)
                malPieData.setDrawValues(false)

                malMediaStatsStatusPieChart.setHoleColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        android.R.color.transparent
                    )
                )
                malMediaStatsStatusPieChart.setDrawEntryLabels(false)
                malMediaStatsStatusPieChart.setTouchEnabled(false)
                malMediaStatsStatusPieChart.description.isEnabled = false
                malMediaStatsStatusPieChart.legend.isEnabled = false
                malMediaStatsStatusPieChart.data = malPieData
                malMediaStatsStatusPieChart.invalidate()
            } catch (e: Exception) {
                DialogUtility.showToast(activity, e.localizedMessage)
            }

            malMediaStatsStatusPieChart.visibility = View.VISIBLE
            malMediaStatsStatusShowButton.visibility = View.GONE

            malMediaStatsStatusRecyclerView.adapter =
                MediaStatsStatusRvAdapter(requireActivity(), malStatusDistributionList)
        }
    }

    private fun handleScoreDistribution() {
        if (mediaData?.stats?.scoreDistribution?.isNullOrEmpty() == true) {
            mediaStatsScoreLayout.visibility = View.GONE
            return
        }

        mediaStatsScoreLayout.visibility = View.VISIBLE

        val barEntries = ArrayList<BarEntry>()
        mediaData?.stats?.scoreDistribution?.forEach {
            val barEntry = BarEntry(it?.score?.toFloat()!!, it.amount?.toFloat()!!)
            barEntries.add(barEntry)
        }

        val barDataSet = BarDataSet(barEntries, "Score Distribution")
        barDataSet.colors = Constant.SCORE_COLOR_LIST

        if (!viewModel.showStatsAutomatically) {
            mediaStatsScoreBarChart.visibility = View.GONE
            mediaStatsScoreShowButton.visibility = View.VISIBLE

            mediaStatsScoreShowButton.setOnClickListener {
                val dialog = ChartDialog()
                val bundle = Bundle()
                bundle.putString(ChartDialog.BAR_ENTRIES, viewModel.gson.toJson(barDataSet))
                dialog.arguments = bundle
                dialog.show(childFragmentManager, null)
            }
        } else {
            try {
                val barData = BarData(barDataSet)
                barData.setValueTextColor(AndroidUtility.getResValueFromRefAttr(activity, R.attr.themeContentColor))
                barData.barWidth = 3F

                mediaStatsScoreBarChart.axisLeft.setDrawGridLines(false)
                mediaStatsScoreBarChart.axisLeft.setDrawAxisLine(false)
                mediaStatsScoreBarChart.axisLeft.setDrawLabels(false)

                mediaStatsScoreBarChart.axisRight.setDrawGridLines(false)
                mediaStatsScoreBarChart.axisRight.setDrawAxisLine(false)
                mediaStatsScoreBarChart.axisRight.setDrawLabels(false)

                mediaStatsScoreBarChart.xAxis.setDrawGridLines(false)
                mediaStatsScoreBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                mediaStatsScoreBarChart.xAxis.setLabelCount(barEntries.size, true)
                mediaStatsScoreBarChart.xAxis.textColor = AndroidUtility.getResValueFromRefAttr(activity, R.attr.themeContentColor)

                mediaStatsScoreBarChart.setTouchEnabled(false)
                mediaStatsScoreBarChart.description.isEnabled = false
                mediaStatsScoreBarChart.legend.isEnabled = false
                mediaStatsScoreBarChart.data = barData
                mediaStatsScoreBarChart.invalidate()
            } catch (e: Exception) {
                DialogUtility.showToast(activity, e.localizedMessage)
            }

            mediaStatsScoreBarChart.visibility = View.VISIBLE
            mediaStatsScoreShowButton.visibility = View.GONE
        }

        if (viewModelSettings.appSettings.fetchFromMal && !failedMal) {
            malMediaStatsScoreLayout.visibility = View.VISIBLE

            val malBarEntries = ArrayList<BarEntry>()
            if (mediaType == MediaType.ANIME) {
                animeStats?.data?.scores?.forEach {
                    val malBarEntry = BarEntry(it.score?.toFloat()!!, it.votes?.toFloat()!!)
                    malBarEntries.add(malBarEntry)
                }
            } else {
                mangaStats?.data?.scores?.forEach {
                    val malBarEntry = BarEntry(it.score?.toFloat()!!, it.votes?.toFloat()!!)
                    malBarEntries.add(malBarEntry)
                }
            }

            val malBarDataSet = BarDataSet(malBarEntries, "Score Distribution")
            malBarDataSet.colors = Constant.SCORE_COLOR_LIST

            try {
                val malBarData = BarData(malBarDataSet)
                malBarData.setValueTextColor(
                    AndroidUtility.getResValueFromRefAttr(
                        activity,
                        R.attr.themeContentColor
                    )
                )
                malBarData.barWidth = .25F

                malMediaStatsScoreBarChart.axisLeft.setDrawGridLines(false)
                malMediaStatsScoreBarChart.axisLeft.setDrawAxisLine(false)
                malMediaStatsScoreBarChart.axisLeft.setDrawLabels(false)

                malMediaStatsScoreBarChart.axisRight.setDrawGridLines(false)
                malMediaStatsScoreBarChart.axisRight.setDrawAxisLine(false)
                malMediaStatsScoreBarChart.axisRight.setDrawLabels(false)

                malMediaStatsScoreBarChart.xAxis.setDrawGridLines(false)
                malMediaStatsScoreBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                malMediaStatsScoreBarChart.xAxis.setLabelCount(barEntries.size, true)
                malMediaStatsScoreBarChart.xAxis.textColor =
                    AndroidUtility.getResValueFromRefAttr(activity, R.attr.themeContentColor)

                malMediaStatsScoreBarChart.setTouchEnabled(false)
                malMediaStatsScoreBarChart.description.isEnabled = false
                malMediaStatsScoreBarChart.legend.isEnabled = false
                malMediaStatsScoreBarChart.data = malBarData
                malMediaStatsScoreBarChart.invalidate()
            } catch (e: Exception) {
                DialogUtility.showToast(activity, e.localizedMessage)
            }

            malMediaStatsScoreBarChart.visibility = View.VISIBLE
            malMediaStatsScoreShowButton.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaStatsRankingRecyclerView.adapter = null
        mediaStatsStatusRecyclerView.adapter = null
    }
}
