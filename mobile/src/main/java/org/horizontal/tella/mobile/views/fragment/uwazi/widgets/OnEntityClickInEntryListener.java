package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import java.util.List;

import org.horizontal.tella.mobile.presentation.uwazi.UwaziRelationShipEntity;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

public interface OnEntityClickInEntryListener {
    void onSelectEntitiesClickedInEntryFragment(UwaziEntryPrompt formEntryPrompt, List<UwaziRelationShipEntity> entitiesNames);
}
