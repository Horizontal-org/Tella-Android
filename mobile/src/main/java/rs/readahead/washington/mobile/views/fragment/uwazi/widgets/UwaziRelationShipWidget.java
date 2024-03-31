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

import org.hzontal.shared_ui.buttons.PanelToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziRelationShipWidget extends UwaziQuestionWidget {
    private final HashMap<String, FormMediaFile> formFiles = new HashMap<>();

    Context context;
    Button addFiles;
    ImageButton clearButton;
    ViewGroup filesPanel;
    PanelToggleButton filesToggle;
    TextView numberOfFiles;
    Boolean isPdf;
    View infoFilePanel;
    private final ArrayList<AppCompatCheckBox> checkBoxes;
    // counter of all checkboxes which can be in result set
    // List of all Ids which can be in result set (Ids from all checkboxes without header checkboxes)
    private final ArrayList<String> checkIds = new ArrayList<>();

    public UwaziRelationShipWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt, boolean isPdf) {
        super(context, formEntryPrompt);
        checkBoxes = new ArrayList<>();
        this.context = context;
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addImageWidgetViews(linearLayout);
        addAnswerView(linearLayout);

        setHelpTextView(formEntryPrompt.getHelpText());
        clearAnswer();
    }


    @Override
    public Object getAnswer() {
        List<String> vc = new ArrayList<>();
        List<UwaziValue> selection = new ArrayList<>();

        for (int i = 0; i < checkBoxes.size(); ++i) {
            AppCompatCheckBox c = checkBoxes.get(i);
            if (c.isChecked()) {
                vc.add(checkIds.get(i));
            }
        }

        if (vc.size() == 0) {
            return null;
        } else {
            for (String answer : vc) {
                UwaziValue uValue = new UwaziValue(answer);
                selection.add(uValue);
            }
            return selection;
        }
    }

    @Override
    public void clearAnswer() {
    }

    @Override
    public void setFocus(Context context) {

    }

    private void addImageWidgetViews(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multifile, linearLayout, true);

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));
        clearButton.setOnClickListener(v -> clearAnswer());

        infoFilePanel = view.findViewById(R.id.infoFilePanel);
        filesPanel = view.findViewById(R.id.files);
        filesToggle = view.findViewById(R.id.toggle_button);
        filesToggle.setVisibility(GONE);
        numberOfFiles = view.findViewById(R.id.numOfFiles);
        addFiles = view.findViewById(R.id.addText);
        addFiles.setText(R.string.select_entities);
        addFiles.setOnClickListener(v -> showSelectEntitiesScreen());
    }


    private void showSelectEntitiesScreen() {
        ((OnSelectEntitiesClickListener) getContext()).onSelectEntitiesClicked();
    }


}
