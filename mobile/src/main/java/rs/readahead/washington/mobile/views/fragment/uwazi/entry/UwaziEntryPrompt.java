package rs.readahead.washington.mobile.views.fragment.uwazi.entry;

import rs.readahead.washington.mobile.data.entity.uwazi.DateOfIncident;

public class UwaziEntryPrompt {
    private String _id;
    private final String formIndex;
    private final String type;
    private final String question;
    private String answer;
    private final Boolean required;
    private final Boolean readonly = false;
    private final String helpText;

    public UwaziEntryPrompt(String formIndex, String type, String question, Boolean required, String helpText) {
        this.formIndex = formIndex;
        this.type = type;
        this.question = question;
        this.required = required;
        this.helpText = helpText;
    }

    public UwaziEntryPrompt(String _id, String formIndex, String type, String question, Boolean required, String helpText) {
        this._id = _id;
        this.formIndex = formIndex;
        this.type = type;
        this.question = question;
        this.required = required;
        this.helpText = helpText;
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

    public int getControlType() {
        return 1;
    }

    public String getDataType() {
        return type;
    }

    public String getIndex() {
        return formIndex;
    }

    public DateOfIncident getAnswerValue() {
        if (answer == null) {
            return new DateOfIncident(0);
        } else {
            return new DateOfIncident(Integer.parseInt(answer));
        }
    }

    public String getID() {
        return _id;
    }

    public void setID(String _id) {
        this._id = _id;
    }
}
