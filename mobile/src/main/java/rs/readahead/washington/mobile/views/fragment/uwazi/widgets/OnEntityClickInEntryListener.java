package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import java.util.List;

import rs.readahead.washington.mobile.presentation.uwazi.UwaziRelationShipEntity;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

public interface OnEntityClickInEntryListener {
    void onSelectEntitiesClickedInEntryFragment(UwaziEntryPrompt formEntryPrompt, List<UwaziRelationShipEntity> entitiesNames);
}
