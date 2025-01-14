package org.horizontal.tella.mobile.views.fragment.uwazi.entry;

import java.util.List;

import org.horizontal.tella.mobile.domain.entity.uwazi.SelectValue;
import org.horizontal.tella.mobile.presentation.uwazi.UwaziRelationShipEntity;

public class UwaziEntryPrompt {
    private String _id;
    private final String formIndex;
    private final String type;
    private String question;
    private String answer;
    private final Boolean required;
    private final Boolean readonly = false;
    private final String helpText;
    private List<SelectValue> selectValues = null;
    private List<UwaziRelationShipEntity> entities = null;

    public UwaziEntryPrompt(String formIndex, String type, String question, Boolean required, String helpText) {
        this.formIndex = formIndex;
        this.type = type;
        this.question = question;
        this.required = required;
        this.helpText = helpText;
    }

    public List<UwaziRelationShipEntity> getEntities() {
        return entities;
    }
    public UwaziEntryPrompt(String _id, List<UwaziRelationShipEntity> entities, String formIndex, String type, String question, Boolean required, String helpText) {
        this._id = _id;
        this.formIndex = formIndex;
        this.type = type;
        this.question = question;
        this.required = required;
        this.helpText = helpText;
        this.entities = entities;
    }

    public UwaziEntryPrompt(String _id, String formIndex, String type, String question, Boolean required, String helpText) {
        this._id = _id;
        this.formIndex = formIndex;
        this.type = type;
        this.question = question;
        this.required = required;
        this.helpText = helpText;
    }

    public UwaziEntryPrompt(String _id, String formIndex, String type, String question, Boolean required, String helpText, List<SelectValue> values) {
        this._id = _id;
        this.formIndex = formIndex;
        this.type = type;
        this.question = question;
        this.required = required;
        this.helpText = helpText;
        this.selectValues = values;
    }

    public String getLongText() {
        return question;
    }

    public Boolean isRequired() {
        return required;
    }

    public Boolean isReadOnly() {
        return readonly;
    }

    public String getHelpText() {
        return helpText;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswerText() {
        if (answer == null)
            return null;
        else {
            return answer;
        }
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getControlType() {
        return 1;
    }

    public String getDataType() {
        return type;
    }

    public String getIndex() {
        return formIndex;
    }

    public String getID() {
        return _id;
    }

    public void setID(String _id) {
        this._id = _id;
    }

    public List<SelectValue> getSelectValues() {
        return selectValues;
    }

    public void setSelectValues(List<SelectValue> selectValues) {
        this.selectValues = selectValues;
    }
}
