package rs.readahead.washington.mobile.views.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import rs.readahead.washington.mobile.views.fragment.BlankFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.DraftFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.OutboxFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.SubmittedFormsListFragment;

public class FormPagerAdapter extends FragmentStateAdapter {
    public static final int BLANK_POSITION = 0;
    public static final int DRAFT_POSITION = 1;
    public static final int OUTBOX_POSITION = 2;
    public static final int SUBMITTED_POSITION = 3;

    public FormPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case BLANK_POSITION:
                return BlankFormsListFragment.newInstance();
            case DRAFT_POSITION:
                return DraftFormsListFragment.newInstance();
            case OUTBOX_POSITION:
                return OutboxFormsListFragment.newInstance();
            case SUBMITTED_POSITION:
                return SubmittedFormsListFragment.newInstance();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
