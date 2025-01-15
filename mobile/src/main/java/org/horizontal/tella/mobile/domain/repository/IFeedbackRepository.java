package org.horizontal.tella.mobile.domain.repository;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import org.horizontal.tella.mobile.domain.entity.Feedback;


public interface IFeedbackRepository {
    Completable sendFeedback(@NonNull Feedback feedback);
}
