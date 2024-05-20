package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import org.hzontal.shared_ui.buttons.PanelToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.uwazi.NestedSelectValue;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziRelationShipEntity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.CollectRelationShipPreviewView;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziRelationShipWidget extends UwaziQuestionWidget {
    private ArrayList<UwaziRelationShipEntity> relationShipEntities = new ArrayList<>();

    Context context;
    Button addEntities;
    ImageButton clearButton;
    ViewGroup entitiesPanel;
    PanelToggleButton entitiesToggle;
    TextView numberOfEntities;
    View infoEntitiesPanel;
    private final List<UwaziRelationShipEntity> items;

    public UwaziRelationShipWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt, boolean isPdf) {
        super(context, formEntryPrompt);
        items = formEntryPrompt.getEntities();

        this.context = context;
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);


        addImageWidgetViews(linearLayout);
        addAnswerView(linearLayout);
        setHelpTextView(getContext().getString(R.string.Uwazi_RelationShipWidget_Select_Entities));
        clearAnswer();
        if (items != null) showPreviewFromDraft();

    }


    @Override
    public ArrayList<UwaziRelationShipEntity> getAnswer() {
        return relationShipEntities;

    }

    @Override
    public void setFocus(Context context) {

    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        ArrayList<UwaziRelationShipEntity> result = new ArrayList<>();

        if (data instanceof String) {
            ArrayList<UwaziRelationShipEntity> resultList = new Gson().fromJson((String) data, new TypeToken<List<UwaziRelationShipEntity>>() {
            }.getType());
            result.addAll(resultList);
        } else {
            for (Object o : (ArrayList) data) {
                if (o instanceof UwaziRelationShipEntity) {
                    result.add((UwaziRelationShipEntity) o);
                } else {
                    String label = Objects.requireNonNull(((LinkedTreeMap) o).get("label")).toString();
                    String value = Objects.requireNonNull(((LinkedTreeMap) o).get("value")).toString();
                    result.add(new UwaziRelationShipEntity(value, label, "entity"));
                }
            }
        }
        if (result != null && !result.isEmpty()) {
            relationShipEntities = (ArrayList<UwaziRelationShipEntity>) result;
            if (!relationShipEntities.isEmpty()) {
                for (UwaziRelationShipEntity relation : relationShipEntities) {
                    if (!relationShipEntities.contains(relation)) {
                        relationShipEntities.add(relation);
                    }
                }
                showPreview();
                return getEntitiesNames().toString();
            }
        }
        return null;
    }


    protected List<UwaziRelationShipEntity> getEntitiesNames() {
        if (relationShipEntities != null) {
            return new ArrayList<>(relationShipEntities);
        } else {
            return null;
        }
    }

    private void showPreview() {
        entitiesPanel.removeAllViews();
        infoEntitiesPanel.setVisibility(VISIBLE);
        clearButton.setVisibility(VISIBLE);
        for (UwaziRelationShipEntity relationShip : relationShipEntities) {
            CollectRelationShipPreviewView previewView = new CollectRelationShipPreviewView(context, null, relationShip);
            entitiesPanel.addView(previewView);
        }
        numberOfEntities.setText(context.getResources().getQuantityString(R.plurals.Uwazi_RelationShipWidget_EntitiesAttached, relationShipEntities.size(), relationShipEntities.size()));
        addEntities.setText(R.string.add_more_entities);
        entitiesToggle.setOpen();
    }

    private void showPreviewFromDraft() {
        entitiesPanel.removeAllViews();
        infoEntitiesPanel.setVisibility(VISIBLE);
        clearButton.setVisibility(VISIBLE);
        relationShipEntities.clear();
        for (UwaziRelationShipEntity entry : items) {
            relationShipEntities.add(entry);
            CollectRelationShipPreviewView previewView = new CollectRelationShipPreviewView(context, null, entry);
            entitiesPanel.addView(previewView);
        }
        numberOfEntities.setText(context.getResources().getQuantityString(R.plurals.Uwazi_RelationShipWidget_EntitiesAttached, relationShipEntities.size(), relationShipEntities.size()));
        addEntities.setText(R.string.select_entities);
        entitiesToggle.setOpen();

    }

    private void addImageWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multifile, linearLayout, true);

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));
        clearButton.setOnClickListener(v -> clearAnswer());

        infoEntitiesPanel= view.findViewById(R.id.infoFilePanel);
        entitiesPanel = view.findViewById(R.id.files);
        entitiesToggle = view.findViewById(R.id.toggle_button);
        entitiesToggle.setVisibility(GONE);
        numberOfEntities = view.findViewById(R.id.numOfFiles);
        addEntities = view.findViewById(R.id.addText);
        addEntities.setText(R.string.select_entities);
        addEntities.setOnClickListener(v -> showSelectEntitiesScreen());
    }

    private void showSelectEntitiesScreen() {
        waitingForAData = true;
        ((OnSelectEntitiesClickListener) getContext()).onSelectEntitiesClicked(formEntryPrompt, getEntitiesNames());
    }

    @Override
    public void clearAnswer() {
        relationShipEntities.clear();
        infoEntitiesPanel.setVisibility(GONE);
        clearButton.setVisibility(View.GONE);
        entitiesPanel.removeAllViews();
        addEntities.setText(R.string.select_entities);
    }

}
