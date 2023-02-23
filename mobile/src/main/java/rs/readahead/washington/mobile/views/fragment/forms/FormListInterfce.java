package rs.readahead.washington.mobile.views.fragment.forms;


public interface FormListInterfce {

    enum Type {
        DRAFT, BLANK, SUBMITTED, OUTBOX
    }

    Type getFormListType();
}
