package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.databinding.OnboardCalculatorFragmentBinding
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.activity.onboarding.OnBoardCalculatorFragment
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class HideBehindCalculator : BaseFragment() {

    private lateinit var binding : OnboardCalculatorFragmentBinding
    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun getInstance(position: Int) = HideBehindCalculator().apply {
            arguments = bundleOf(ARG_POSITION to position)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = OnboardCalculatorFragmentBinding.inflate(layoutInflater,container,false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // initView(view)
        val position = requireArguments().getInt(ARG_POSITION)
        // val onBoardingTitles = requireContext().resources.getStringArray(R.array.onboarding_titles)
        // val onBoardingTexts = requireContext().resources.getStringArray(R.array.onboarding_texts)
        with(binding) {
           // sheetSubtitle.text = "test $position"
        }
    }
    override fun initView(view: View) {
      //  (activity as OnFragmentSelected?)?.hideAppbar()

//        view.findViewById<View>(R.id.back_btn).setOnClickListener {
//            activity.onBackPressed()
//        }
//
//        view.findViewById<View>(R.id.calculatorView).setOnClickListener {
//        }
//
//        view.findViewById<View>(R.id.calculatorImg).setOnClickListener {
//        }
//
//        view.findViewById<View>(R.id.calculatorBtn).setOnClickListener {
//            confirmHideBehindCalculator()
//        }
    }


}