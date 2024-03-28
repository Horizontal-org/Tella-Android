package rs.readahead.washington.mobile.bus.event;

import java.util.Iterator;
import java.util.List;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityModel;
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityStatus;

public class RecentBackgroundActivitiesEvent implements IEvent {

    private List<BackgroundActivityModel> backgroundActivityModels;


    public RecentBackgroundActivitiesEvent(List<BackgroundActivityModel> backgroundActivityModels) {
        this.backgroundActivityModels = backgroundActivityModels;

        removeFinishedItems();
    }

    public List<BackgroundActivityModel> getBackgroundActivityModels() {
        return backgroundActivityModels;
    }

    public void setBackgroundActivityModels(List<BackgroundActivityModel> backgroundActivityModels) {
        this.backgroundActivityModels = backgroundActivityModels;
    }

    // Method to remove finished items from the list
    public void removeFinishedItems() {
        Iterator<BackgroundActivityModel> iterator = backgroundActivityModels.iterator();
        while (iterator.hasNext()) {
            BackgroundActivityModel activityModel = iterator.next();
            if (activityModel.getStatus() == BackgroundActivityStatus.COMPLETED) {
                iterator.remove();
            }
        }
    }

    // Method to check if the list is not empty
    public boolean hasItems() {
        return !backgroundActivityModels.isEmpty();
    }


    public int size() {
        return backgroundActivityModels.size();
    }

}
