package rs.readahead.washington.mobile.domain.repository;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import rs.readahead.washington.mobile.domain.entity.Feedback;


public interface IFeedbackRepository {
    Completable sendFeedback(@NonNull Feedback feedback);
}
