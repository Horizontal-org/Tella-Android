package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import java.util.List;

import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

public interface OnSelectEntitiesClickListenerFromEntry {
    void onSelectEntitiesClickedInEntryFragment(UwaziEntryPrompt formEntryPrompt, List<String> entitiesNames);
}
