package rs.readahead.washington.mobile.javarosa;

import android.text.TextUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import androidx.annotation.NonNull;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.odk.exception.JavaRosaException;
import rs.readahead.washington.mobile.views.collect.CollectFormView;
import timber.log.Timber;

public class FormParser implements IFormParserContract.IFormParser {
    private IFormParserContract.IView view;
    private final FormController formController;

    private FormEntryPrompt[] prompts;
    private FormEntryCaption[] groups;

    private final String locationFieldPrefix;
    private final String metadataFieldPrefix;

    private enum Direction {
        PREVIOUS,
        CURRENT,
        NEXT
    }


    public FormParser(IFormParserContract.IView suppliedView) {
        this.view = suppliedView;
        this.formController = FormController.getActive();
        this.locationFieldPrefix = suppliedView.getContext().getString(R.string.tella_location_field_prefix);
        this.metadataFieldPrefix = suppliedView.getContext().getString(R.string.tella_metadata_field_prefix);
    }

    @Override
    public void parseForm() {
        init();
        parse(Direction.CURRENT);
    }

    @Override
    public void stepToNextScreen() {
        parse(Direction.NEXT);
    }

    @Override
    public void stepToPrevScreen() {
        parse(Direction.PREVIOUS);
    }

    @Override
    public boolean isFirstScreen() {
        boolean first;

        try {
            first = formController.stepToPreviousScreenEvent() == FormEntryController.EVENT_BEGINNING_OF_FORM;
            formController.stepToNextScreenEvent();

            if (formController.getEvent() == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                first = false;
                formController.stepToNextScreenEvent();
            }
        } catch (JavaRosaException e) {
            first = false;
            Timber.e(e, this.getClass().getName());
        }

        return first;
    }

    @Override
    public boolean isFormChanged() {
        return formController.isFormChanged();
    }

    @Override
    public void startFormChangeTracking() {
        formController.initFormChangeTracking();
    }

    @Override
    public boolean isFormFinal() {
        CollectFormInstance instance = formController.getCollectFormInstance();

        return instance == null || instance.getStatus().isFinal();
    }

    @Override
    public void executeRepeat() {
        try {
            formController.newRepeat();
        } catch (Exception e) {
            view.formParseError(e);
            return;
        }

        if (!formController.indexIsInFieldList()) {
            stepToNextScreen();
        } else {
            parse(Direction.CURRENT);
        }
    }

    @Override
    public void cancelRepeat() {
        stepToNextScreen();
    }

    @Override
    public void setWidgetMediaFile(@NonNull String name, @NonNull VaultFile vaultFile) {
        formController.getCollectFormInstance().setWidgetMediaFile(name, FormMediaFile.fromMediaFile(vaultFile));
    }

    @Override
    public void removeWidgetMediaFile(String name) {
        formController.getCollectFormInstance().removeWidgetMediaFile(name);
    }

    @Override
    public void stopWaitingBinaryData() {
        formController.setIndexWaitingForData(null);
    }

    public void setTellaMetadataFields(CollectFormView cfv, Metadata metadata) {
        findMetadataFields(formController.getIndexWaitingForData(), (type, formIndex) -> {
            switch (type) {
                case LOCATION:
                    if (metadata != null && metadata.getMyLocation() != null) {
                        cfv.setBinaryData(formIndex, metadata.getMyLocation());
                    } else {
                        cfv.clearBinaryData(formIndex);
                    }
                    break;

                case METADATA:
                    if (metadata != null) {
                        cfv.setBinaryData(formIndex, FormUtils.formatMetadata(cfv.getContext(), metadata));
                    } else {
                        cfv.clearBinaryData(formIndex);
                    }
                    break;
            }
        });
    }

    public void clearTellaMetadataFields(CollectFormView cfv) {
        findMetadataFields(formController.getIndexWaitingForData(),
                (type, formIndex) -> cfv.clearBinaryData(formIndex));
    }

    public void clearTellaMetadataFields(FormIndex binary, CollectFormView cfv) {
        findMetadataFields(binary, (type, formIndex) -> cfv.clearBinaryData(formIndex));
    }

    public boolean isFormEnd() {
        return formController.getEvent() == FormEntryController.EVENT_END_OF_FORM;
    }

    @Override
    public void destroy() {
        view = null;
    }

    private void init() {
        boolean enableDelete = false;

        CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

        if (instance.getStatus().equals(CollectFormInstanceStatus.DRAFT) ||
                instance.getClonedId() > 0) {
            enableDelete = true;
        }

        view.formPropertiesChecked(enableDelete);
    }

    private void parse(Direction direction) {
        try {
            int event;

            switch (direction) {
                case PREVIOUS:
                    event = formController.stepToPreviousScreenEvent();
                    break;

                case NEXT:
                    event = formController.stepToNextScreenEvent();
                    break;

                default:
                    event = formController.getEvent();
            }

            switch (event) {
                case FormEntryController.EVENT_BEGINNING_OF_FORM:
                    view.formBeginning(formController.getFormTitle());
                    break;

                case FormEntryController.EVENT_END_OF_FORM:
                    view.formEnd(formController.getCollectFormInstance());
                    break;

                case FormEntryController.EVENT_QUESTION:
                    getViewParameters();
                    view.formQuestion(prompts, groups);
                    break;

                case FormEntryController.EVENT_GROUP:
                    getViewParameters();
                    view.formGroup(prompts, groups);
                    break;

                case FormEntryController.EVENT_REPEAT:
                    getViewParameters();
                    view.formRepeat(prompts, groups);
                    break;

                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    view.formPromptNewRepeat(formController.getLastRepeatCount(), formController.getLastGroupText());
                    break;
            }
        } catch (Exception e) {
            viewFormParseError(e);
        }
    }

    private void viewFormParseError(Throwable throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable);
        view.formParseError(throwable);
    }

    private void getViewParameters() {
        prompts = formController.getQuestionPrompts();
        groups = formController.getGroupsForCurrentIndex();
    }

    private void findMetadataFields(FormIndex binary, MetadataFieldFunction function) {
        String binaryName = binary.getReference().getNameLast();

        if (TextUtils.isEmpty(binaryName)) {
            return;
        }

        FormEntryPrompt[] prompts = formController.getQuestionPrompts();
        for (FormEntryPrompt fep : prompts) {
            try {
                String fepName = fep.getIndex().getReference().getNameLast();
                if (isGeoPoint(fep) && (locationFieldPrefix + binaryName).equals(fepName)) {
                    function.found(MetadataFieldFunction.Type.LOCATION, fep.getIndex());
                    break;
                }
                if (isTextField(fep) && (metadataFieldPrefix + binaryName).equals(fepName)) {
                    function.found(MetadataFieldFunction.Type.METADATA, fep.getIndex());
                    break;
                }
            } catch (Exception e) {
                Timber.d(e);
            }
        }
    }

    private boolean isGeoPoint(FormEntryPrompt fep) {
        return fep.getControlType() == Constants.CONTROL_INPUT &&
                fep.getDataType() == Constants.DATATYPE_GEOPOINT;
    }

    private boolean isTextField(FormEntryPrompt fep) {
        return fep.getControlType() == Constants.CONTROL_INPUT &&
                fep.getDataType() == Constants.DATATYPE_TEXT;
    }

    private interface MetadataFieldFunction {
        enum Type {
            LOCATION,
            METADATA
        }

        void found(Type type, FormIndex formIndex);
    }
}
