package org.horizontal.tella.mobile.domain.entity.uwazi;

import java.util.ArrayList;
import java.util.List;

import org.horizontal.tella.mobile.domain.entity.IErrorBundle;

public class ListTemplateResult {
    private List<IErrorBundle> errors = new ArrayList<>();
    private List<UwaziTemplate> templates = new ArrayList<>();


    public ListTemplateResult() {
    }

    public ListTemplateResult(List<UwaziTemplate> templates) {
        this.templates = templates;
    }

    public List<IErrorBundle> getErrors() {
        return errors;
    }

    public void setErrors(List<IErrorBundle> errors) {
        this.errors = errors;
    }

    public List<UwaziTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<UwaziTemplate> templates) {
        this.templates = templates;
    }
}