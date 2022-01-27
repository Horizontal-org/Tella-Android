package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.databinding.FragmentOutboxUwaziBinding

class OutboxUwaziFragment : UwaziListFragment() {

    private var binding : FragmentOutboxUwaziBinding? = null

    override fun getFormListType(): Type {
        return Type.OUTBOX
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOutboxUwaziBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding?.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}