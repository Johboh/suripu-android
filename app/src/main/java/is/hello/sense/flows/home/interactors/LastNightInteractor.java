package is.hello.sense.flows.home.interactors;


import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

@Singleton
public class LastNightInteractor extends ValueInteractor<Timeline> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<Timeline> timeline = subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Timeline> provideUpdateObservable() {
        return apiService.timelineForDate(DateFormatter.lastNight().toString(ApiService.DATE_FORMAT));
    }

}