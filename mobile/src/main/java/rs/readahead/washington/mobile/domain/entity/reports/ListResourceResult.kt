package rs.readahead.washington.mobile.domain.entity.reports

import rs.readahead.washington.mobile.domain.entity.IErrorBundle
import rs.readahead.washington.mobile.domain.entity.reports.ResourceTemplate

class ListResourceResult {
    private var errors: List<IErrorBundle> = ArrayList()
    private var templates: List<ResourceTemplate> = ArrayList()


    fun ListTemplateResult() {}

    fun ListTemplateResult(templates: List<ResourceTemplate>) {
        this.templates = templates
    }

    fun getErrors(): List<IErrorBundle>? {
        return errors
    }

    fun setErrors(errors: List<IErrorBundle>) {
        this.errors = errors
    }

    fun getTemplates(): List<ResourceTemplate>? {
        return templates
    }

    fun setTemplates(templates: List<ResourceTemplate>) {
        this.templates = templates
    }
}