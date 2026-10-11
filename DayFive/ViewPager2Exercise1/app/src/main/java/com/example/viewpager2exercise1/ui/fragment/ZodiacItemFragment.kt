package com.example.viewpager2exercise1.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.viewpager2exercise1.R
import com.example.viewpager2exercise1.databinding.FragmentZodiacItemBinding

class ZodiacItemFragment : Fragment() {
    private var binding : FragmentZodiacItemBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentZodiacItemBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadImageFromArgs()
    }

    // tạo phương thức loadImageFromArgs() để tỉa hình ảnh từ bundle
    private fun loadImageFromArgs(){
        val drawableId = arguments?.getInt(BUNDLE_KEY_DRAWABLE_ID)
        Glide.with(this)
            .load(drawableId)
            .error(R.drawable.dragon)
            .into(binding?.image!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    companion object{
        const val BUNDLE_KEY_DRAWABLE_ID = "pro.branium.drawableId"
    }
}