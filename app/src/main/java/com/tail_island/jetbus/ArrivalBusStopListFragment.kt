package com.tail_island.jetbus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearSmoothScroller
import com.tail_island.jetbus.adapter.BusStopAdapter
import com.tail_island.jetbus.adapter.IndexAdapter
import com.tail_island.jetbus.databinding.FragmentArrivalBusStopListBinding
import com.tail_island.jetbus.view_model.ArrivalBusStopListViewModel
import javax.inject.Inject

class ArrivalBusStopListFragment: Fragment() {
    @Inject lateinit var viewModelProviderFactory: AppViewModelProviderFactory

    private val viewModel: ArrivalBusStopListViewModel by viewModels { viewModelProviderFactory }

    private val args: ArrivalBusStopListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity!!.application as App).component.inject(this)

        viewModel.departureBusStopName.value = args.departureBusStopName

        return FragmentArrivalBusStopListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            recyclerView.adapter = BusStopAdapter().apply {
                viewModel.arrivalBusStops.observe(viewLifecycleOwner, Observer {
                    submitList(it)
                })

                onBusStopClick = {
                    findNavController().navigate(ArrivalBusStopListFragmentDirections.arrivalBusStopListToBusApproachListAction(viewModel.departureBusStop.value!!.name, it.name))
                }
            }

            indexRecyclerView.adapter = IndexAdapter().apply {
                viewModel.arrivalBusStopIndices.observe(viewLifecycleOwner, Observer {
                    submitList(it)
                })

                onIndexClick = {
                    recyclerView.layoutManager!!.startSmoothScroll(
                        object: LinearSmoothScroller(context) {
                            private var isFirstScroll = true

                            override fun updateActionForInterimTarget(action: Action) {
                                if (isFirstScroll) {
                                    super.updateActionForInterimTarget(action)
                                    isFirstScroll = false
                                } else {
                                    action.jumpTo(targetPosition)
                                }
                            }

                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START
                            }
                        }.apply {
                            targetPosition = busStopPosition(viewModel.arrivalBusStops.value!!, it)
                        }
                    )
                }
            }
        }.root
    }
}
