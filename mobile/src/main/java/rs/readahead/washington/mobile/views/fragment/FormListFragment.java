package rs.readahead.washington.mobile.views.fragment;

import androidx.fragment.app.Fragment;


public abstract class FormListFragment extends Fragment {
    public enum Type {
        DRAFT, BLANK, OUTBOX, SUBMITTED
    }

    public abstract Type getFormListType();
}
