package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.fragment.app.Fragment

abstract class UwaziListFragment : Fragment() {

    enum class Type {
        DRAFT, TEMPLATES, SUBMITTED, OUTBOX
    }

    abstract fun getFormListType(): Type
}