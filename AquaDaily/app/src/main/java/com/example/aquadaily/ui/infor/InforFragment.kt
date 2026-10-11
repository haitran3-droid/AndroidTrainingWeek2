package com.example.aquadaily.ui.infor

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aquadaily.R
import com.example.aquadaily.databinding.FragmentInforBinding

/**
 * Fragment hiển thị màn hình danh mục bài viết kiến thức và lời khuyên bổ ích.
 *
 * Chức năng:
 * - Hiển thị các chuyên mục kiến thức lồng nhau (vertical RecyclerView chứa horizontal RecyclerView).
 * - Mở [ArticleDialogFragment] xem chi tiết nội dung khi người dùng chọn một bài viết.
 * - Quản lý việc lưu và khôi phục trạng thái cuộn của cả danh sách cha và các danh sách con khi tạm dừng (`onPause`)
 *   hoặc lưu trạng thái thể hiện (`onSaveInstanceState`).
 */
class InforFragment : Fragment(R.layout.fragment_infor) {
    private var binding: FragmentInforBinding? = null
    private var adapter: InfoSectionAdapter? = null
    private val model: InforViewModel by viewModels {
        viewModelFactory {
            initializer {
                InforViewModel(
                    createSavedStateHandle()
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentInforBinding.bind(view)
        binding = ui
        val manager = LinearLayoutManager(requireContext())
        ui.sections.layoutManager = manager
        adapter = InfoSectionAdapter(
            RecyclerView.RecycledViewPool(),
            model::readScroll,
            model::saveScroll
        ) { article ->
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(ArticleDialogFragment.TAG) == null) {
                ArticleDialogFragment.newInstance(article.id).showNow(fm, ArticleDialogFragment.TAG)
            }
        }.also {
            ui.sections.adapter = it
            it.submitList(model.sections) {
                model.readOuterScroll()?.let(manager::onRestoreInstanceState)
            }
        }
    }

    /** Lưu trạng thái cuộn của cả danh sách con và danh sách cha vào ViewModel. */
    private fun saveScroll() {
        adapter?.saveAllScroll()
        binding?.sections?.layoutManager?.onSaveInstanceState()?.let(model::saveOuterScroll)
    }

    override fun onPause() {
        saveScroll()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveScroll()
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        saveScroll()
        binding?.sections?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }
}