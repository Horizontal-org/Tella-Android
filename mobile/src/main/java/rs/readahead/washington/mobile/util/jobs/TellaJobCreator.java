package rs.readahead.washington.mobile.util.jobs;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;


public class TellaJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case PendingFormSendJob.TAG:
                return new PendingFormSendJob();

            case TellaUploadJob.TAG:
                return new TellaUploadJob();

            default:
                return null;
        }
    }
}
