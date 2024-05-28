package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.fragment.app.Fragment
import rs.readahead.washington.mobile.databinding.FragmentOutboxUwaziBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

abstract class UwaziListFragment : BaseBindingFragment<FragmentOutboxUwaziBinding>(
    FragmentOutboxUwaziBinding::inflate)
{
    enum class Type {
        DRAFT, TEMPLATES, SUBMITTED, OUTBOX
    }

    abstract fun getFormListType(): Type
}