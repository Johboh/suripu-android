package is.hello.sense.zendesk;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.zendesk.sdk.feedback.ZendeskFeedbackConfiguration;
import com.zendesk.sdk.feedback.impl.ZendeskFeedbackConnector;
import com.zendesk.sdk.model.CreateRequest;
import com.zendesk.sdk.model.CustomField;
import com.zendesk.sdk.model.Request;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.sdk.network.impl.ZendeskRequestProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.InternalPrefManager;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class TicketsInteractor extends Interactor {
    /**
     * Included on builds where {@link BuildConfig#IS_BETA} is <code>true</code>.
     * <p>
     * Literal value <code>android_beta</code> suggested by Marina.
     */
    private static final String TAG_BETA = "android_beta";
    private static final long CUSTOM_FIELD_ID_TOPIC = 24321669L;

    @Inject
    Context context;
    @Inject
    ApiService apiService;

    // Request doesn't implement Serializable,
    // so we can't use ValueInteractor<T>. Yay.
    public final InteractorSubject<List<Request>> tickets = InteractorSubject.create();
    private
    @NonNull
    Subscription updateSubscription = Subscriptions.empty();

    //region Lifecycle

    @Override
    protected boolean onForgetDataForLowMemory() {
        tickets.forget();

        return true;
    }

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    //endregion


    //region Updating

    public void update() {
        this.updateSubscription.unsubscribe();
        final Observable<List<Request>> updateObservable = ZendeskHelper.doAction(context, apiService.getAccount(false), callback -> {
            ZendeskRequestProvider provider = new ZendeskRequestProvider();
            provider.getRequests("new,open,pending,hold,solved", callback);
        });
        this.updateSubscription = updateObservable.subscribe(new SafeObserverWrapper<>(tickets));
    }

    //endregion


    public Observable<ZendeskConfig> initializeIfNeeded() {
        logEvent("initializeIfNeeded()");

        return ZendeskHelper.initializeIfNeeded(context, apiService.getAccount(false));
    }

    public Observable<CreateRequest> createTicket(@NonNull final SupportTopic onTopic,
                                                  @NonNull final String text,
                                                  @NonNull final List<String> attachmentTokens) {
        logEvent("createTicket()");

        return ZendeskHelper.doAction(context, apiService.getAccount(false), callback -> {
            final CustomField topicId = new CustomField(CUSTOM_FIELD_ID_TOPIC, onTopic.topic);
            ZendeskConfig.INSTANCE.setCustomFields(Lists.newArrayList(topicId));

            final ZendeskFeedbackConfiguration configuration = new ZendeskFeedbackConfiguration() {
                @Override
                public List<String> getTags() {
                    final List<String> tags = new ArrayList<>();
                    tags.add(ZendeskHelper.sanitizeTag(Build.MODEL));
                    tags.add(ZendeskHelper.sanitizeTag(Build.VERSION.RELEASE));
                    if (BuildConfig.IS_BETA) {
                        tags.add(TAG_BETA);
                    }
                    return tags;
                }

                @Override
                public String getAdditionalInfo() {
                    final String accountId = InternalPrefManager.getAccountId(context);
                    return String.format(Locale.US, "Id: %s\nSense Id: %s",
                                         accountId, Analytics.getSenseId());
                }

                @Override
                public String getRequestSubject() {
                    return "Android Ticket for Sense " + BuildConfig.VERSION_NAME;
                }
            };

            final ZendeskFeedbackConnector connector = new ZendeskFeedbackConnector(context, configuration);
            connector.sendFeedback(text, attachmentTokens, callback);
        });
    }
}
