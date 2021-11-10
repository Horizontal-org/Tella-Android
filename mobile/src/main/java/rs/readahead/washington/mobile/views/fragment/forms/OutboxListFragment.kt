package rs.readahead.washington.mobile.views.fragment.forms

class OutboxListFragment  : FormListFragment(){



    override fun getFormListType(): Type {
        return  Type.OUTBOX
    }
}