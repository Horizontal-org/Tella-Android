package rs.readahead.washington.mobile.views.fragment;

import android.support.v4.app.Fragment;


public abstract class FormListFragment extends Fragment {
    public enum Type {
        DRAFT, BLANK, SUBMITTED
    }

    public abstract Type getFormListType();
}
