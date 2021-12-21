package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.databinding.FragmentUwaziBinding
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class UwaziFragment : BaseFragment() {

    private lateinit var binding: FragmentUwaziBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUwaziBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initView(view: View) {
    }

}