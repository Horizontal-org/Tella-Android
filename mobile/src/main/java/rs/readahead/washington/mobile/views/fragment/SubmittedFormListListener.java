package rs.readahead.washington.mobile.views.fragment;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;

public interface SubmittedFormListListener {
    void showOptionsBottomSheet(final CollectFormInstance instance);
}
