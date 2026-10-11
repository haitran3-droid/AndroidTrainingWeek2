package com.example.viewpager2exercise1.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.viewpager2exercise1.app.ZodiacApp
import com.example.viewpager2exercise1.databinding.FragmentZodiacBinding
import com.example.viewpager2exercise1.ui.ZodiacViewModel
import com.example.viewpager2exercise1.ui.adapter.ZodiacPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class ZodiacFragment : Fragment() {
    private var binding: FragmentZodiacBinding? = null
    private lateinit var adapter: ZodiacPagerAdapter
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private var isTabLayoutMediatorAttached = false
    private val viewModel: ZodiacViewModel by viewModels {
        ZodiacViewModel.Factory(
            (requireActivity().application as ZodiacApp).getRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentZodiacBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewPager()
        observerZodiac()
    }

    private fun initViewPager() {
        adapter = ZodiacPagerAdapter(this)
        binding?.viewPager?.adapter = adapter
    }

    private fun initTabLayoutMediator() {
        tabLayoutMediator = TabLayoutMediator(
            binding?.tabLayout!!,
            binding?.viewPager!!
        ) { tab, position ->
            val zodiac = viewModel.getZodiacAtPosition(position)
            if (zodiac != null) {
                tab.text = zodiac.name
            }
        }
        tabLayoutMediator.attach()
    }

    private fun observerZodiac() {
        viewModel.zodiacs.observe(viewLifecycleOwner) { zodiacs ->
            adapter.submitList(zodiacs)
            if (!isTabLayoutMediatorAttached) {
                initTabLayoutMediator()
                isTabLayoutMediatorAttached = true
            } else {
                for (i in 0 until binding?.tabLayout?.tabCount!!) {
                    val tab = binding?.tabLayout?.getTabAt(i)
                    val zodiac = viewModel.getZodiacAtPosition(i)
                    if (tab != null && zodiac != null) {
                        tab.text = zodiac.name
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::tabLayoutMediator.isInitialized && isTabLayoutMediatorAttached) {
            tabLayoutMediator.detach()
        }
    }
}