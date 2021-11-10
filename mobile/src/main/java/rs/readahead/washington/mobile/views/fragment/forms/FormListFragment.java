package rs.readahead.washington.mobile.views.fragment.forms;

import androidx.fragment.app.Fragment;


public abstract class FormListFragment extends Fragment {
    public enum Type {
        DRAFT, BLANK, SUBMITTED,OUTBOX
    }

    public abstract Type getFormListType();
}
