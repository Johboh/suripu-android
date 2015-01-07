package is.hello.sense.api.sessions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class TransientApiSessionManager extends ApiSessionManager {
    private OAuthSession session;

    public TransientApiSessionManager(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void storeOAuthSession(@Nullable OAuthSession session) {
        this.session = session;
    }

    @Override
    protected @Nullable OAuthSession retrieveOAuthSession() {
        return this.session;
    }
}
