package org.horizontal.tella.mobile.views.fragment.uwazi

import org.horizontal.tella.mobile.databinding.FragmentOutboxUwaziBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

abstract class UwaziListFragment : BaseBindingFragment<FragmentOutboxUwaziBinding>(
    FragmentOutboxUwaziBinding::inflate
) {
    enum class Type {
        DRAFT, TEMPLATES, SUBMITTED, OUTBOX
    }

    abstract fun getFormListType(): Type
}