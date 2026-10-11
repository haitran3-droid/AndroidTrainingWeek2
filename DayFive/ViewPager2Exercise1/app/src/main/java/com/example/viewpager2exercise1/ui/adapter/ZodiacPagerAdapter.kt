package com.example.viewpager2exercise1.ui.adapter

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.viewpager2exercise1.data.model.Zodiac
import com.example.viewpager2exercise1.ui.fragment.ZodiacItemFragment
import com.example.viewpager2exercise1.ui.fragment.ZodiacItemFragment.Companion.BUNDLE_KEY_DRAWABLE_ID

class ZodiacPagerAdapter(fragment : Fragment) : FragmentStateAdapter(fragment){
    private val listAdapter = object : ListAdapter<Zodiac, RecyclerView.ViewHolder>(DIFF_CALLBACK){
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            throw UnsupportedOperationException("Not used")
        }

        override fun onBindViewHolder(
            p0: RecyclerView.ViewHolder,
            p1: Int
        ) {
        }

    }
    //ghi đè phương thức createFragment() taaoj lập fragment cho từng tab trong ViewPager2
    override fun createFragment(p0: Int): Fragment {
        val fragment = ZodiacItemFragment()
        val args = Bundle()
        val zodiac = listAdapter.currentList[p0]
        if(zodiac != null && zodiac.drawableId != null){
            args.putInt(BUNDLE_KEY_DRAWABLE_ID, zodiac.drawableId)
        }
        fragment.arguments = args
        return fragment
    }

    override fun getItemId(position: Int): Long{
        val drawableId = listAdapter.currentList[position].drawableId
        return drawableId?.hashCode()?.toLong() ?: NO_ID
    }

    override fun getItemCount(): Int {
        return listAdapter.currentList.size
    }

    override fun containsItem(itemId: Long): Boolean {
        return listAdapter.currentList
            .mapNotNull { it.drawableId }
            .map{it.hashCode().toLong()}
            .any{it == itemId}
    }

    fun submitList(newList: List<Zodiac>){
        listAdapter.submitList(newList)
    }

    companion object {
        const val NO_ID = -1L
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Zodiac>(){
            override fun areItemsTheSame(
                p0: Zodiac,
                p1: Zodiac
            ): Boolean {
                return p0.drawableId == p1.drawableId
            }

            override fun areContentsTheSame(
                p0: Zodiac,
                p1: Zodiac
            ): Boolean {
                return p0 == p1
            }

        }
    }
}