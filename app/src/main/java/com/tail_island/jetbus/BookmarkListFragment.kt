package com.tail_island.jetbus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tail_island.jetbus.adapter.BookmarkAdapter
import com.tail_island.jetbus.databinding.FragmentBookmarkListBinding
import com.tail_island.jetbus.view_model.BookmarkListViewModel
import javax.inject.Inject

class BookmarkListFragment: Fragment() {
    @Inject lateinit var viewModelProviderFactory: AppViewModelProviderFactory

    private val viewModel: BookmarkListViewModel by viewModels { viewModelProviderFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity!!.application as App).component.inject(this)

        return FragmentBookmarkListBinding.inflate(inflater, container, false).apply {
            addButton.setOnClickListener {
                findNavController().navigate(BookmarkListFragmentDirections.bookmarkListToDepartureBusStopListAction())
            }

            recyclerView.adapter = BookmarkAdapter().apply {
                viewModel.bookmarks.observe(viewLifecycleOwner, Observer {
                    submitList(it)
                })

                onBookmarkClick = {
                    findNavController().navigate(BookmarkListFragmentDirections.bookmarkListToBusApproachListAction(it.departureBusStopName, it.arrivalBusStopName))
                }
            }
        }.root
    }
}
