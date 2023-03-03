package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.OnboardCalculatorFragmentBinding
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import kotlin.properties.Delegates

class HideBehindCalculator : BaseFragment() {

    private lateinit var binding: OnboardCalculatorFragmentBinding

    companion object {
        private const val VIEWPAGER_POSITION = "VIEWPAGER_POSITION"
        fun getInstance(position: Int) = HideBehindCalculator().apply {
            arguments = bundleOf(VIEWPAGER_POSITION to position)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View{
        binding = OnboardCalculatorFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        val position = requireArguments().getInt(VIEWPAGER_POSITION)
        var drawables = resources.obtainTypedArray(R.array.calculator_skin_array)
        with(binding) {
            this.calculatorImg.setImageDrawable(drawables.getDrawable(position))
        }
    }


}