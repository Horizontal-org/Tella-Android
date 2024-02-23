package rs.readahead.washington.mobile.domain.entity.resources

import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse

class ListResourceResult {
    var errors: List<Throwable> = ArrayList()
    var slugs: List<ProjectSlugResourceResponse> = ArrayList()

    constructor()
    constructor(slugs: List<ProjectSlugResourceResponse>) {
        this.slugs = slugs
    }
}