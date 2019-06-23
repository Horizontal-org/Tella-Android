package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.TrainModule;


public class DeleteTrainModuleEvent implements IEvent {
    private final TrainModule module;

    public DeleteTrainModuleEvent(TrainModule module) {
        this.module = module;
    }

    public TrainModule getModule() {
        return module;
    }
}
