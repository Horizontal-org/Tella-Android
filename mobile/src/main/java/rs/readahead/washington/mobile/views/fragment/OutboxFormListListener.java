package rs.readahead.washington.mobile.views.fragment;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;

public interface OutboxFormListListener {
    void showOptionsBottomSheet(final CollectFormInstance instance);
}
