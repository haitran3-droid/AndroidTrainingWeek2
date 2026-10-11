package com.example.aquadaily.ui.infor

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aquadaily.databinding.ItemInfoSectionBinding

/**
 * Adapter hiển thị danh sách các chuyên mục kiến thức ([InfoSection]) cuộn dọc.
 *
 * Mỗi chuyên mục chứa một RecyclerView con cuộn ngang ([ArticleAdapter]).
 *
 * Điểm tối ưu kiến trúc & hiệu năng:
 * - Sử dụng chung một [RecyclerView.RecycledViewPool] giữa tất cả các RecyclerView con cuộn ngang để tiết kiệm bộ nhớ.
 * - Tự động lưu và khôi phục vị trí cuộn ngang của từng chuyên mục thông qua `saveScroll` và `readScroll`.
 * - Tự động tính toán chiều cao RecyclerView con dựa trên `fontScale` để không bị cắt chữ khi người dùng phóng to font hệ thống.
 * - Sử dụng biến thế hệ `generation` token để tránh xung đột khôi phục cuộn khi ViewHolder bị tái sử dụng nhanh.
 */
class InfoSectionAdapter(
    private val pool: RecyclerView.RecycledViewPool,
    private val readScroll: (Long) -> Parcelable?,
    private val saveScroll: (Long, Parcelable) -> Unit,
    private val onArticle: (Article) -> Unit
) : ListAdapter<InfoSection, InfoSectionAdapter.Holder>(Diff) {
    private val activeHolders = mutableSetOf<Holder>()

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    inner class Holder(val ui: ItemInfoSectionBinding) : RecyclerView.ViewHolder(ui.root) {
        private val cards = ArticleAdapter(onArticle)
        private val manager = LinearLayoutManager(ui.root.context, RecyclerView.HORIZONTAL, false)
        private var sectionId: Long? = null
        private var generation = 0
        private var ready = false

        init {
            ViewCompat.setAccessibilityHeading(ui.sectionTitle, true)
            manager.initialPrefetchItemCount = 3
            ui.articles.layoutManager = manager
            ui.articles.adapter = cards
            ui.articles.setRecycledViewPool(pool)
            ui.articles.setHasFixedSize(true)

            // Điều chỉnh chiều cao thẻ bài viết theo fontScale để đảm bảo khả năng tiếp cận (Accessibility)
            val scale = ui.root.resources.configuration.fontScale
            ui.articles.layoutParams = ui.articles.layoutParams.apply {
                height =
                    ((216 + 80 * (scale - 1).coerceAtLeast(0f)) * ui.root.resources.displayMetrics.density).toInt()
            }

            ui.articles.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) save()
                }
            })
        }

        fun bind(section: InfoSection) {
            save()
            ready = false
            sectionId = section.id
            val token = ++generation
            ui.sectionTitle.setText(section.title)
            cards.submitList(section.articles) {
                if (generation != token || sectionId != section.id) return@submitList
                val restored = readScroll(section.id)
                if (restored != null) {
                    manager.onRestoreInstanceState(restored)
                } else {
                    manager.scrollToPositionWithOffset(0, 0)
                }
                ui.articles.postOnAnimation {
                    if (generation == token) ready = true
                }
            }
        }

        fun save() {
            val id = sectionId ?: return
            if (ready) manager.onSaveInstanceState()?.let { saveScroll(id, it) }
        }

        fun recycle() {
            save()
            ready = false
            generation++
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInfoSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        activeHolders.add(holder)
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: Holder) {
        holder.recycle()
        activeHolders.remove(holder)
        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: Holder) {
        holder.save()
        super.onViewDetachedFromWindow(holder)
    }

    /** Lưu lại vị trí cuộn của tất cả các ViewHolder đang hiển thị. */
    fun saveAllScroll() = activeHolders.forEach { it.save() }

    private object Diff : DiffUtil.ItemCallback<InfoSection>() {
        override fun areItemsTheSame(oldItem: InfoSection, newItem: InfoSection) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InfoSection, newItem: InfoSection) =
            oldItem == newItem
    }
}