package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;

import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


public abstract class UwaziFileBinaryWidget extends UwaziQuestionWidget {
    private FormMediaFile file;
    private String filename;
    /*private String fileId;*/

    public UwaziFileBinaryWidget(Context context, @NonNull UwaziEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);
    }

    @Override
    public void clearAnswer() {
        //MyApplication.bus().post(new MediaFileBinaryWidgetCleared(formEntryPrompt.getIndex(), filename));
    }

    @Override
    public IAnswerData getAnswer() {
        return TextUtils.isEmpty(getFilename()) ? null : new StringData(getFilename());
    }

    protected FormMediaFile getFile() {
        return file;
    }

    protected String getFilename() {
        if (file != null) {
            return file.name;
        } else {
            return null;
        }
    }

    protected void setFile(FormMediaFile file) {
        this.file = file;
    }

    protected void setFilename(String filename) {
        if (file != null) {
            this.file.name = filename;
        }

        this.filename = filename;
    }

    public String getFileId() {
        return file.id;
    }

    /*public void setFileId(String fileId) {
        this.fileId = fileId;
    }*/
}

