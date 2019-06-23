package rs.readahead.washington.mobile.domain.entity.collect;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.domain.entity.IErrorBundle;


public class ListFormResult {
    private List<IErrorBundle> errors = new ArrayList<>();
    private List<CollectForm> forms = new ArrayList<>();


    public ListFormResult() {
    }

    public ListFormResult(List<CollectForm> forms) {
        this.forms = forms;
    }

    public List<IErrorBundle> getErrors() {
        return errors;
    }

    public void setErrors(List<IErrorBundle> errors) {
        this.errors = errors;
    }

    public List<CollectForm> getForms() {
        return forms;
    }

    public void setForms(List<CollectForm> forms) {
        this.forms = forms;
    }
}
