package com.tail_island.jetbus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.tail_island.jetbus.adapter.BusApproachAdapter
import com.tail_island.jetbus.databinding.FragmentBusApproachListBinding
import com.tail_island.jetbus.view_model.BusApproachListViewModel
import javax.inject.Inject

class BusApproachListFragment: Fragment() {
    @Inject lateinit var viewModelProviderFactory: AppViewModelProviderFactory

    private val viewModel: BusApproachListViewModel by viewModels { viewModelProviderFactory }

    private val args: BusApproachListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity!!.application as App).component.inject(this)

        viewModel.departureBusStopName.value = args.departureBusStopName
        viewModel.arrivalBusStopName.value = args.arrivalBusStopName

        return FragmentBusApproachListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            viewModel = this@BusApproachListFragment.viewModel

            bookmarkImageView.setOnClickListener {
                this@BusApproachListFragment.viewModel.toggleBookmark()
            }

            recyclerView.adapter = BusApproachAdapter().apply {
                this@BusApproachListFragment.viewModel.busApproaches.observe(viewLifecycleOwner, Observer {
                    emptyView.visibility = if (it.isEmpty()) { View.VISIBLE } else { View.GONE }

                    submitList(it)
                })
            }
        }.root
    }
}
