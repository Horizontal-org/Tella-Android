package org.horizontal.tella.mobile.views.collect.widgets;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.bus.event.MediaFileBinaryWidgetCleared;

public abstract class MediaFileBinaryWidget extends QuestionWidget {
    private String filename;
    private String fileId;

    public MediaFileBinaryWidget(Context context, @NonNull FormEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);
    }

    @Override
    public void clearAnswer() {
        MyApplication.bus().post(new MediaFileBinaryWidgetCleared(formEntryPrompt.getIndex(), filename));
    }

    @Override
    public IAnswerData getAnswer() {
        return TextUtils.isEmpty(getFilename()) ? null : new StringData(getFilename());
    }

    protected String getFilename() {
        return filename;
    }

    protected void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
