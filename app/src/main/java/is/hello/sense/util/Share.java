package is.hello.sense.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.segment.analytics.Properties;

import java.io.IOException;
import java.io.OutputStream;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * An abstraction over common intents used for sharing
 * that attempts to remove most of the sharp corners.
 */
public abstract class Share {
    //region Base

    protected final Intent intent;

    protected Share(@NonNull final String action) {
        this.intent = new Intent(action);
    }

    public abstract void send(@NonNull Activity from);

    public abstract void send(@NonNull Fragment from);

    //endregion


    //region Factories

    public static Text text(@NonNull final String text) {
        return new Text(text);
    }

    public static Image image(@NonNull final Bitmap bitmap) {
        return new Image(bitmap);
    }

    public static Email email(@NonNull final String address) {
        return new Email(address);
    }

    //endregion


    //region Implementations

    public static class Text extends Share {
        private Properties properties = null;

        public Text(@NonNull final String text) {
            super(Intent.ACTION_SEND);
            intent.setType("text/plain");

            intent.putExtra(Intent.EXTRA_TEXT, text);
        }

        public Text withProperties(@NonNull final Properties properties) {
            this.properties = properties;
            return this;
        }

        public Text withSubject(@NonNull final String subject) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            return this;
        }

        @Override
        public void send(@Nullable final Activity from) {
            if (from == null) {
                return;
            }
            from.startActivity(Intent.createChooser(intent, from.getString(R.string.action_share)));
            if (properties != null) {
                Analytics.trackEvent(Analytics.Global.EVENT_SHARE, properties);
            }
        }

        @Override
        public void send(@NonNull final Fragment from) {
            send(from.getActivity());

        }
    }

    public static class Email extends Share {
        public Email(@NonNull final String emailAddress) {
            super(Intent.ACTION_SENDTO);
            intent.setData(Uri.fromParts("mailto", emailAddress, null));
        }

        public Email withSubject(@NonNull final String subject) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            return this;
        }

        public Email withBody(@NonNull final String body) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
            return this;
        }

        public Email withAttachment(@NonNull final Uri attachment) {
            intent.putExtra(Intent.EXTRA_STREAM, attachment);
            return this;
        }

        @Override
        public void send(@NonNull final Activity from) {
            try {
                from.startActivity(intent);
            } catch (final ActivityNotFoundException e) {
                final SenseAlertDialog alertDialog = new SenseAlertDialog(from);
                alertDialog.setTitle(R.string.dialog_error_title);
                alertDialog.setMessage(R.string.error_no_email_client);
                alertDialog.setPositiveButton(android.R.string.ok, null);
                alertDialog.show();
            }
        }

        @Override
        public void send(@NonNull final Fragment from) {

        }
    }

    public static class Image extends Share {
        private final Bitmap bitmap;
        private
        @Nullable
        SenseDialogFragment loadingDialogFragment;

        private Image(@NonNull final Bitmap bitmap) {
            super(Intent.ACTION_SEND);
            intent.setType("image/jpeg");

            this.bitmap = bitmap;
        }

        public Image withTitle(@Nullable final String title) {
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            return this;
        }

        public Image withDescription(@Nullable final String description) {
            intent.putExtra(Intent.EXTRA_TEXT, description);
            return this;
        }

        public Image withLoadingDialog(@NonNull final SenseDialogFragment loadingDialogFragment) {
            this.loadingDialogFragment = loadingDialogFragment;
            return this;
        }

        @Override
        public void send(@NonNull final Fragment from) {
            final ExternalStoragePermission externalStoragePermission = new ExternalStoragePermission(from);
            if (!externalStoragePermission.isGranted()) {
                externalStoragePermission.requestPermissionWithDialog();
                return;
            }
            final ContentResolver contentResolver = from.getActivity().getContentResolver();
            final ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, intent.getStringExtra(Intent.EXTRA_SUBJECT));
            values.put(MediaStore.Images.Media.DESCRIPTION, intent.getStringExtra(Intent.EXTRA_TEXT));
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            final Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            //todo unsafe usage of Observable.create needs refactoring to use Observable.defer
            final Observable<Void> writeImage = Observable.create(s -> {
                OutputStream imageOut = null;
                try {
                    imageOut = contentResolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageOut);
                    imageOut.flush();
                } catch (IOException e) {
                    s.onError(e);
                } finally {
                    Functions.safeClose(imageOut);
                    bitmap.recycle();
                }

                s.onNext(null);
                s.onCompleted();
            });

            writeImage.subscribeOn(Schedulers.io())
                      .observeOn(Rx.mainThreadScheduler())
                      .subscribe(ignored -> {
                          intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                          from.startActivity(Intent.createChooser(intent, from.getString(R.string.action_share)));

                          if (loadingDialogFragment != null) {
                              loadingDialogFragment.dismissSafely();
                          }
                      }, e -> {
                          Logger.error(Share.class.getSimpleName(), "Could not share bitmap image", e);
                          ErrorDialogFragment.presentError(from.getActivity(), e);
                          if (loadingDialogFragment != null) {
                              loadingDialogFragment.dismissSafely();
                          }
                      });
        }


        @Override
        public void send(@NonNull final Activity from) {

        }
    }

    //endregion
    public static Properties getInsightProperties(@NonNull final String category) {
        final Properties properties = new Properties();
        properties.put(Analytics.Global.PROP_TYPE, Analytics.Global.PROP_INSIGHT);
        properties.put(Analytics.Global.PROP_INSIGHT_CATEGORY, category);
        return properties;
    }
}
