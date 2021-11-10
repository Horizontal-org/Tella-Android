package rs.readahead.washington.mobile.views.fragment.forms

import android.view.View

class OutboxListFragment  : FormListFragment(){

    override fun getFormListType(): Type {
        return  Type.OUTBOX
    }
}