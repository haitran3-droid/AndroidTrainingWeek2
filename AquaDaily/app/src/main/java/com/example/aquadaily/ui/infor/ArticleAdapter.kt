package com.example.aquadaily.ui.infor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aquadaily.R
import com.example.aquadaily.databinding.ItemArticleBinding

/**
 * Adapter hiển thị danh sách các thẻ bài viết ([Article]) cuộn ngang trong từng chuyên mục.
 *
 * @param onArticle Callback được gọi khi người dùng nhấn chọn một bài viết.
 */
class ArticleAdapter(private val onArticle: (Article) -> Unit) :
    ListAdapter<Article, ArticleAdapter.Holder>(Diff) {
    class Holder(val ui: ItemArticleBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder =
            Holder(ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        // Thiết lập accessibility để trình đọc màn hình (TalkBack) nhận diện thẻ bài viết như một Button có thể click
        ViewCompat.setAccessibilityDelegate(
            holder.itemView,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: android.view.View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.Button"
                }
            }
        )
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val article = getItem(position)
        holder.ui.title.setText(article.title)
        holder.ui.illustration.setImageResource(article.art)
        holder.ui.illustration.setBackgroundColor(holder.itemView.context.getColor(article.background))
        holder.ui.root.contentDescription =
            holder.itemView.context.getString(article.title) + ", " + holder.itemView.context.getString(
                R.string.article_detail
            )
        // Gán lại onClick listener vì ViewHolder có thể được tái sử dụng qua shared RecycledViewPool
        holder.ui.root.setOnClickListener { onArticle(article) }
    }

    private object Diff : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem
    }
}