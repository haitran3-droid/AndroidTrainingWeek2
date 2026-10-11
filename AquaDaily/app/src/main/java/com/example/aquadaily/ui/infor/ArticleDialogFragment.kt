package com.example.aquadaily.ui.infor

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.aquadaily.R
import com.example.aquadaily.databinding.DialogArticleBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * DialogFragment hiển thị chi tiết nội dung, tiêu đề và hình minh họa của một bài viết mẹo sức khỏe.
 */
class ArticleDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val article = requireNotNull(ArticleCatalog.find(requireArguments().getLong("article_id")))
        val ui = DialogArticleBinding.inflate(layoutInflater)
        ui.illustration.setImageResource(article.art)
        ui.illustration.setBackgroundColor(requireContext().getColor(article.background))
        ui.body.setText(article.body)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(article.title)
            .setView(ui.root)
            .setPositiveButton(R.string.close, null)
            .create()
    }

    companion object {
        const val TAG = "article_detail"

        /** Tạo mới dialog với ID của bài viết cần xem. */
        fun newInstance(id: Long) =
            ArticleDialogFragment().apply { arguments = bundleOf("article_id" to id) }
    }
}