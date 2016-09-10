package is.hello.sense.mvp.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class PresenterView {
    protected final Context context;
    protected View view;

    public PresenterView(@NonNull final Activity activity) {
        this.context = activity;
    }

    protected final String getString(@StringRes final int res) {
        return context.getString(res);
    }

    public void attach() {

    }

    public void create() {

    }

    public void resume() {

    }

    @NonNull
    public abstract View createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState);

    public void viewCreated() {

    }

    public void pause() {

    }

    public void destroyView() {

    }

}
