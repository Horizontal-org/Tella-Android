package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentTemplatesUwaziBinding
import rs.readahead.washington.mobile.databinding.FragmentUwaziBinding
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.ViewPagerAdapter


class TemplatesUwaziFragment : UwaziListFragment() {

    private lateinit var binding: FragmentTemplatesUwaziBinding

    override fun getFormListType(): Type {
        return Type.TEMPLATES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplatesUwaziBinding.inflate(inflater, container, false)
        return binding.root
    }

}