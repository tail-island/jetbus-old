package com.tail_island.jetbus

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tail_island.jetbus.databinding.FragmentSplashBinding
import com.tail_island.jetbus.view_model.SplashViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class SplashFragment: Fragment() {
    @Inject lateinit var viewModelProviderFactory: AppViewModelProviderFactory

    private val viewModel: SplashViewModel by viewModels { viewModelProviderFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity!!.application as App).component.inject(this)

        return FragmentSplashBinding.inflate(inflater, container, false).root
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            viewModel.syncDatabase() ?: activity!!.finish()

            findNavController().navigate(SplashFragmentDirections.splashFragmentToBookmarkListFragment())
        }
    }
}
