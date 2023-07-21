package rs.readahead.washington.mobile.views.fragment.forms;


public interface FormListInterface {

    enum Type {
        DRAFT, BLANK, SUBMITTED, OUTBOX
    }

    Type getFormListType();
}
