package rs.readahead.washington.mobile.views.fragment.uwazi.entry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.uwazi.UwaziConstants;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziFileBinaryWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziQuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziWidgetFactory;


@SuppressLint("ViewConstructor")
public class UwaziFormView extends LinearLayout {
    public static final String FIELD_LIST = "field-list";

    // starter random number for view IDs
    private static final int VIEW_ID = 12961974;

    private final ArrayList<UwaziQuestionWidget> widgets;


    public UwaziFormView(Context context, final UwaziEntryPrompt[] questionPrompts) {
        super(context);

        widgets = new ArrayList<>();

        inflate(context, R.layout.collect_form_view, this);

        // set my layout
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        setOrientation(VERTICAL);

        // set padding
        int padding = getResources().getDimensionPixelSize(R.dimen.collect_form_padding_vertical);
        setPadding(0, padding, 0, padding);

        TextView formViewTitle = findViewById(R.id.formViewTitle);
        /*if (getGroupTitle(groups).length() > 0) {
            formViewTitle.setVisibility(VISIBLE);
            formViewTitle.setText(getGroupTitle(groups));
        } else {
            formViewTitle.setVisibility(GONE);
        }*/

        // prepare widgets layout
        LinearLayout.LayoutParams widgetLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // when the grouped fields are populated by an external app, this will get true.
        boolean readOnlyOverride = false;

        // external app handling removed..
        LayoutInflater inflater = LayoutInflater.from(getContext());

        int id = 0;
        for (UwaziEntryPrompt p : questionPrompts) {
            if (id > 0) {
                LinearLayout separator = new LinearLayout(getContext());
                inflater.inflate(R.layout.collect_form_delimiter, separator, true);
                addView(separator, widgetLayout);
            }

            if (p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_TEXT) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_NUMERIC) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_MEDIA) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_IMAGE) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_DATE) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_DATERANGE) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_GEOLOCATION) ||
                    p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_MARKDOWN)
                    || p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_LINK)
                    || p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_SELECT)
                    || p.getDataType().equals(UwaziConstants.UWAZI_DATATYPE_MULTISELECT)
            ) {
                UwaziQuestionWidget qw = UwaziWidgetFactory.createWidgetFromPrompt(p, getContext(), readOnlyOverride);
                qw.setId(VIEW_ID + id++);
                widgets.add(qw);
                addView(qw, widgetLayout);
            }
        }
    }

    public void setFocus(Context context) {
        if (widgets.size() > 0) {
            widgets.get(0).setFocus(context);
        }
    }

    public void setValidationConstraintText(String formIndex, String text) {
        for (UwaziQuestionWidget q : widgets) {
            if (q.getPrompt().getID().equals(formIndex)) {
                q.setConstraintValidationText(text);
                break;
            }
        }
    }

    public void clearValidationConstraints() {
        for (UwaziQuestionWidget q : widgets) {
            q.setConstraintValidationText(null);
        }
    }

    public LinkedHashMap<String, UwaziValue> getAnswers() {
        LinkedHashMap<String, UwaziValue> answers = new LinkedHashMap<>();

        for (UwaziQuestionWidget q : widgets) {
            UwaziEntryPrompt p = q.getPrompt();
            answers.put(p.getID(), q.getAnswer());
        }

        return answers;
    }

    public List<FormMediaFile> getFiles() {
        List<FormMediaFile> files = new ArrayList<>();
        for (UwaziQuestionWidget widget : widgets) {
            if (widget instanceof UwaziFileBinaryWidget) {
                files.add(((UwaziFileBinaryWidget) widget).getFile());
            }
        }
        return files;
    }

    public String setBinaryData(@NonNull Object data) {
        for (UwaziQuestionWidget q : widgets) {
            if (q.waitingForAData) {
                try {
                    return q.setBinaryData(data);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error attaching data", Toast.LENGTH_LONG).show();
                } finally {
                    q.waitingForAData = false;
                }
            }
        }

        return null;
    }

    public void setBinaryData(String formIndex, @NonNull Object data) {
        for (UwaziQuestionWidget q : widgets) {
            if (q.getPrompt().getID().equals(formIndex)) {
                try {
                    q.setBinaryData(data);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error attaching data", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Nullable
    public String clearBinaryData() {
        for (UwaziQuestionWidget q : widgets) {
            if (isWaitingForBinaryData(q)) {
                String name = q.getBinaryName();
                q.clearAnswer();
                return name;
            }
        }

        return null;
    }

    public void clearBinaryData(String formIndex) {
        for (UwaziQuestionWidget q : widgets) {
            if (q.getPrompt().getIndex().equals(formIndex)) {
                q.clearAnswer();
                break;
            }
        }
    }

    private boolean isWaitingForBinaryData(UwaziQuestionWidget q) {
        return q.getPrompt().getIndex().equals(
                FormController.getActive().getIndexWaitingForData()
        );
    }
}
