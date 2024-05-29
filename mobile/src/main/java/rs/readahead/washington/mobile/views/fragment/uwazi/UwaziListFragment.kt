package rs.readahead.washington.mobile.views.fragment.uwazi

import rs.readahead.washington.mobile.databinding.FragmentOutboxUwaziBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

abstract class UwaziListFragment : BaseBindingFragment<FragmentOutboxUwaziBinding>(
    FragmentOutboxUwaziBinding::inflate
) {
    enum class Type {
        DRAFT, TEMPLATES, SUBMITTED, OUTBOX
    }

    abstract fun getFormListType(): Type
}