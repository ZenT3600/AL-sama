package it.matteoleggio.alchan.ui.mangalist.list

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import it.matteoleggio.alchan.R
import it.matteoleggio.alchan.data.response.MediaList
import it.matteoleggio.alchan.helper.Constant
import it.matteoleggio.alchan.helper.libs.GlideApp
import it.matteoleggio.alchan.helper.pojo.ListStyle
import it.matteoleggio.alchan.helper.roundToOneDecimal
import it.matteoleggio.alchan.helper.utils.AndroidUtility
import it.matteoleggio.alchan.helper.utils.DialogUtility
import kotlinx.android.synthetic.main.list_manga_list_linear.view.*
import kotlinx.android.synthetic.main.list_title.view.*
import type.MediaFormat
import type.ScoreFormat

class MangaListRvAdapter(private val context: Context,
                         private val list: List<MediaList>,
                         private val scoreFormat: ScoreFormat,
                         private val listStyle: ListStyle?,
                         private val listener: MangaListListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_TITLE = 0
        const val VIEW_TYPE_ITEM = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TITLE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_title, parent, false)
            TitleViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_manga_list_linear, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TitleViewHolder) {
            val item = list[position]
            holder.titleText.text = item.notes
            if (listStyle?.textColor != null) {
                holder.titleText.setTextColor(Color.parseColor(listStyle.textColor))
            }
        } else if (holder is ItemViewHolder) {
            val mediaList = list[position]

            GlideApp.with(context).load(mediaList.media?.coverImage?.large).into(holder.mangaCoverImage)
            holder.mangaTitleText.text = mediaList.media?.title?.userPreferred
            holder.mangaFormatText.text = mediaList.media?.format?.name?.replace('_', ' ')

            if (scoreFormat == ScoreFormat.POINT_3) {
                GlideApp.with(context).load(AndroidUtility.getSmileyFromScore(mediaList.score)).into(holder.mangaStarIcon)
                holder.mangaStarIcon.imageTintList = ColorStateList.valueOf(AndroidUtility.getResValueFromRefAttr(context, R.attr.themePrimaryColor))
                holder.mangaRatingText.text = ""
            } else {
                GlideApp.with(context).load(R.drawable.ic_star_filled).into(holder.mangaStarIcon)
                holder.mangaStarIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.yellowStar))
                holder.mangaRatingText.text = if (mediaList.score == null || mediaList.score == 0.0) {
                    "?"
                } else {
                    mediaList.score?.roundToOneDecimal()
                }
            }

            holder.mangaProgressBar.progress = if (mediaList.media?.chapters == null && mediaList.progress!! > 0) {
                50
            } else if (mediaList.media?.chapters != null && mediaList.media?.chapters != 0) {
                (mediaList.progress!!.toDouble() / mediaList.media?.chapters!!.toDouble() * 100).toInt()
            } else {
                0
            }

            holder.mangaProgressVolumesText.text = "${mediaList.progressVolumes}/${mediaList.media?.volumes ?: '?'}"
            holder.mangaIncrementProgressVolumesButton.visibility = if ((mediaList.media?.volumes == null || mediaList.media?.volumes!! > mediaList.progressVolumes!!) && mediaList.progressVolumes!! < 65535) View.VISIBLE else View.GONE

            holder.mangaProgressText.text = "${mediaList.progress}/${mediaList.media?.chapters ?: '?'}"
            holder.mangaIncrementProgressButton.visibility = if ((mediaList.media?.chapters == null || mediaList.media?.chapters!! > mediaList.progress!!) && mediaList.progress!! < 65535) View.VISIBLE else View.GONE

            if (mediaList.media?.format == MediaFormat.MANGA || mediaList.media?.format == MediaFormat.ONE_SHOT) {
                holder.mangaProgressVolumesLayout.visibility = if (listStyle?.hideMangaVolume == true) View.GONE else View.VISIBLE
                holder.mangaProgressLayout.visibility = if (listStyle?.hideMangaChapter == true) View.GONE else View.VISIBLE
            } else if (mediaList.media?.format == MediaFormat.NOVEL) {
                holder.mangaProgressVolumesLayout.visibility = if (listStyle?.hideNovelVolume == true) View.GONE else View.VISIBLE
                holder.mangaProgressLayout.visibility = if (listStyle?.hideNovelChapter == true) View.GONE else View.VISIBLE
            }

            holder.itemView.setOnClickListener {
                listener.openEditor(mediaList.id)
            }

            holder.mangaIncrementProgressButton.setOnClickListener {
                listener.incrementProgress(mediaList)
            }

            holder.mangaProgressText.setOnClickListener {
                listener.openProgressDialog(mediaList)
            }

            holder.mangaIncrementProgressVolumesButton.setOnClickListener {
                listener.incrementProgress(mediaList, true)
            }

            holder.mangaProgressVolumesText.setOnClickListener {
                listener.openProgressDialog(mediaList, true)
            }

            holder.mangaRatingText.setOnClickListener {
                listener.openScoreDialog(mediaList)
            }

            holder.mangaStarIcon.setOnClickListener {
                listener.openScoreDialog(mediaList)
            }

            holder.mangaTitleText.setOnClickListener {
                listener.openBrowsePage(mediaList.media!!)
            }

            holder.mangaCoverImage.setOnClickListener {
                listener.openBrowsePage(mediaList.media!!)
            }

            holder.itemView.setOnLongClickListener {
                if (listStyle?.longPressViewDetail == true) {
                    listener.showDetail(mediaList.id)
                    true
                } else {
                    false
                }
            }

            if (listStyle?.showNotesIndicator == true && !mediaList.notes.isNullOrBlank()) {
                holder.mangaNotesLayout.visibility = View.VISIBLE
                holder.mangaNotesLayout.setOnClickListener {
                    DialogUtility.showInfoDialog(context, mediaList.notes ?: "")
                }
            } else {
                holder.mangaNotesLayout.visibility = View.GONE
            }

            if (listStyle?.showPriorityIndicator == true && mediaList.priority != null && mediaList.priority != 0) {
                holder.mangaPriorityIndicator.visibility = View.VISIBLE
                holder.mangaPriorityIndicator.setBackgroundColor(Constant.PRIORITY_COLOR_MAP[mediaList.priority!!]!!)
            } else {
                holder.mangaPriorityIndicator.visibility = View.GONE
            }

            if (listStyle?.hideScoreWhenNotScored == true && (mediaList.score == null || mediaList.score == 0.0)) {
                holder.mangaStarIcon.visibility = View.GONE
                holder.mangaRatingText.visibility = View.GONE
            } else {
                holder.mangaStarIcon.visibility = View.VISIBLE
                holder.mangaRatingText.visibility = View.VISIBLE
            }

            if (listStyle?.cardColor != null) {
                holder.listCardBackground.setCardBackgroundColor(Color.parseColor(listStyle.cardColor))

                // 6 is color hex code length without the alpha
                val transparentCardColor =  "#CC" + listStyle.cardColor?.substring(listStyle.cardColor?.length!! - 6)
                holder.mangaNotesLayout.setCardBackgroundColor(Color.parseColor(transparentCardColor))
            }

            if (listStyle?.primaryColor != null) {
                holder.mangaTitleText.setTextColor(Color.parseColor(listStyle.primaryColor))
                holder.mangaRatingText.setTextColor(Color.parseColor(listStyle.primaryColor))
                holder.mangaProgressText.setTextColor(Color.parseColor(listStyle.primaryColor))
                holder.mangaIncrementProgressButton.strokeColor = ColorStateList.valueOf(Color.parseColor(listStyle.primaryColor))
                holder.mangaIncrementProgressButton.setTextColor(Color.parseColor(listStyle.primaryColor))
                if (scoreFormat == ScoreFormat.POINT_3) {
                    holder.mangaStarIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(listStyle.primaryColor))
                }

                holder.mangaProgressVolumesText.setTextColor(Color.parseColor(listStyle.primaryColor))
                holder.mangaIncrementProgressVolumesButton.strokeColor = ColorStateList.valueOf(Color.parseColor(listStyle.primaryColor))
                holder.mangaIncrementProgressVolumesButton.setTextColor(Color.parseColor(listStyle.primaryColor))
                if (scoreFormat == ScoreFormat.POINT_3) {
                    holder.mangaStarIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(listStyle.primaryColor))
                }
            }

            if (listStyle?.secondaryColor != null) {
                holder.mangaProgressBar.progressTintList = ColorStateList.valueOf(Color.parseColor(listStyle.secondaryColor))
                val transparentSecondary = listStyle.secondaryColor?.substring(0, 1) + "80" + listStyle.secondaryColor?.substring(1)
                holder.mangaProgressBar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor(transparentSecondary))
            }

            if (listStyle?.textColor != null) {
                holder.mangaFormatText.setTextColor(Color.parseColor(listStyle.textColor))
                holder.mangaNotesIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(listStyle.textColor))
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].id == 0) VIEW_TYPE_TITLE else VIEW_TYPE_ITEM
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listCardBackground = view.listCardBackground!!
        val mangaTitleText = view.mangaTitleText!!
        val mangaCoverImage = view.mangaCoverImage!!
        val mangaFormatText = view.mangaFormatText!!
        val mangaStarIcon = view.mangaStarIcon!!
        val mangaRatingText = view.mangaRatingText!!
        val mangaProgressBar = view.mangaProgressBar!!
        val mangaProgressLayout = view.mangaProgressLayout!!
        val mangaProgressText = view.mangaProgressText!!
        val mangaIncrementProgressButton = view.mangaIncrementProgressButton!!
        val mangaProgressVolumesLayout = view.mangaProgressVolumesLayout!!
        val mangaProgressVolumesText = view.mangaProgressVolumesText!!
        val mangaIncrementProgressVolumesButton = view.mangaIncrementProgressVolumesButton!!
        val mangaNotesLayout = view.mangaNotesLayout!!
        val mangaNotesIcon = view.mangaNotesIcon!!
        val mangaPriorityIndicator = view.mangaPriorityIndicator!!
    }

    class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText = view.titleText!!
    }
}