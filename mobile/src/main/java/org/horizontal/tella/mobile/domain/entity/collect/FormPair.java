package org.horizontal.tella.mobile.domain.entity.collect;

import org.javarosa.core.model.FormDef;

public class FormPair {
    private final CollectForm form;
    private final FormDef formDef;

    public FormPair(CollectForm form, FormDef formDef) {
        this.formDef = formDef;
        this.form = form;
    }

    public CollectForm getForm() {
        return form;
    }

    public FormDef getFormDef() {
        return formDef;
    }
}
