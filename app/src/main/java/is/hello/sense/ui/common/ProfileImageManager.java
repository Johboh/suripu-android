package is.hello.sense.ui.common;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.ArrayList;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Fetch;
import is.hello.sense.util.FilePathUtil;
import is.hello.sense.util.ImageUtil;
import is.hello.sense.util.Logger;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.util.Analytics.ProfilePhoto.Source;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.CAMERA;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.FACEBOOK;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.GALLERY;
import static is.hello.sense.util.Fetch.Image.REQUEST_CODE_CAMERA;
import static is.hello.sense.util.Fetch.Image.REQUEST_CODE_GALLERY;

/**
 * Must be instantiated by {@link is.hello.sense.ui.common.ProfileImageManager.Builder}
 */
public class ProfileImageManager {
    private static final int REQUEST_CODE_OPTIONS = 5;
    private static final int OPTION_ID_FROM_FACEBOOK = 0;
    private static final int OPTION_ID_FROM_CAMERA = 1;
    private static final int OPTION_ID_FROM_GALLERY = 2;
    private static final int OPTION_ID_REMOVE_PICTURE = 4;

    private static final String PREFS = "profile_image_manager_prefs";
    private static final String PHOTO_URI_KEY = "PHOTO_URI_KEY";
    private static final String PHOTO_SOURCE_KEY = "PHOTO_SOURCE_KEY";

    private final int minOptions;
    private final Fragment fragment;
    private final ImageUtil imageUtil;
    private final FilePathUtil filePathUtil;
    private final ExternalStoragePermission permission;
    private final ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
    private final SenseBottomSheet.Option deleteOption;
    private final SharedPreferences preferences;

    private boolean showOptions;

    private ProfileImageManager(@NonNull final Fragment fragment,
                                @NonNull final ImageUtil imageUtil,
                                @NonNull final FilePathUtil filePathUtil) {
        final Context context = fragment.getActivity();
        this.preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.fragment = fragment;
        this.imageUtil = imageUtil;
        this.filePathUtil = filePathUtil;
        this.permission = new ExternalStoragePermission(fragment);
        this.showOptions = true;
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_FACEBOOK)
                        .setTitle(R.string.action_import_from_facebook)
                        .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                        .setIcon(R.drawable.facebook_logo)
                   );
        if (imageUtil.hasDeviceCamera()) {
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_FROM_CAMERA)
                            .setTitle(R.string.action_take_photo)
                            .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                            .setIcon(R.drawable.settings_camera)
                       );
            minOptions = 3;
        } else {
            minOptions = 2;
        }
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_GALLERY)
                        .setTitle(R.string.action_import_from_gallery)
                        .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                        .setIcon(R.drawable.settings_photo_library)
                   );
        deleteOption = new SenseBottomSheet.Option(OPTION_ID_REMOVE_PICTURE)
                .setTitle(R.string.action_remove_picture)
                .setTitleColor(ContextCompat.getColor(context, R.color.error_text))
                .setIcon(R.drawable.icon_alarm_delete);
    }

    public void showPictureOptions() {
        if (!showOptions) {
            return;
        }
        final BottomSheetDialogFragment options = BottomSheetDialogFragment.newInstance(this.options);
        options.setTargetFragment(fragment, REQUEST_CODE_OPTIONS);
        options.showAllowingStateLoss(fragment.getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    public void hidePictureOptions() {
        final BottomSheetDialogFragment options = (BottomSheetDialogFragment) fragment.getFragmentManager()
                                                                                      .findFragmentByTag(BottomSheetDialogFragment.TAG);
        if (options != null) {
            options.dismissSafely();
        }
    }

    public void addDeleteOption() {
        if (options.size() == minOptions) {
            options.add(deleteOption);
        }
    }

    public void removeDeleteOption() {
        if (options.size() == minOptions + 1) {
            options.remove(deleteOption);
        }
    }

    /**
     * @return true if requestCode matched.
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, @NonNull final Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA || requestCode == REQUEST_CODE_GALLERY) {
                setShowOptions(true);
                return true;
            } else {
                return false;
            }
        }

        if (requestCode == REQUEST_CODE_OPTIONS) {
            setShowOptions(false);
            final int optionID = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, -1);
            handlePictureOptionSelection(optionID);
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            final Uri photoUri = getUri();
            if (Uri.EMPTY.equals(photoUri)) {
                setShowOptions(true);
                //Todo display error that the image uri was lost somehow
                //getListener().onImageUriFetchError(new Throwable(), R.string.error_account_fetch_image_uri_message, R.string.error_account_fetch_image_uri_title );
                return true;
            }
            saveSource(CAMERA); // in case the user took a few days to return.
            getListener().onFromCamera(photoUri);
        } else if (requestCode == REQUEST_CODE_GALLERY) {
            saveSource(GALLERY);
            final Uri imageUri = data.getData();
            getListener().onFromGallery(imageUri);
        } else {
            return false;
        }
        return true;
    }

    public void setShowOptions(final boolean showOptions) {
        this.showOptions = showOptions;
    }

    private Observable<TypedFile> compressImageObservable(@Nullable final Uri imageUri) {
        if (imageUri == null || Uri.EMPTY.equals(imageUri)) {
            return Observable.error(new Throwable("No valid filePath given"));
        }

        final String localPath = filePathUtil.getLocalPath(imageUri);
        final boolean mustDownload = !filePathUtil.isFoundOnDevice(localPath);
        final String path = mustDownload ? imageUri.toString() : localPath;

        return imageUtil.provideObservableToCompressFile(path, mustDownload)
                        .map(file -> new TypedFile("multipart/form-data", file))
                        .doOnError(Functions.LOG_ERROR)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Rx.mainThreadScheduler());
    }

    /**
     * Trim cache and preferences
     */
    public void clear() {
        imageUtil.trimCache();
        preferences.edit().clear().apply();
    }

    //region permission checks

    public void onRequestPermissionResult(final int requestCode,
                                          @NonNull final String[] permissions,
                                          @NonNull final int[] grantResults) {
        if (permission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            handleGalleryOption();
        } else {
            permission.showEnableInstructionsDialogForGallery();
            setShowOptions(true);
        }
    }

    //endregion

    //region Camera Options

    private void handlePictureOptionSelection(final int optionID) {
        switch (optionID) {
            case OPTION_ID_FROM_FACEBOOK:
                handleFacebookOption();
                break;
            case OPTION_ID_FROM_CAMERA:
                handleCameraOption();
                break;
            case OPTION_ID_FROM_GALLERY:
                handleGalleryOption();
                break;
            case OPTION_ID_REMOVE_PICTURE:
                handleRemoveOption();
                break;
            default:
                Logger.warn(ProfileImageManager.class.getSimpleName(), "unknown picture option selected");
        }
    }

    private void handleFacebookOption() {
        saveSource(FACEBOOK);
        getListener().onImportFromFacebook();
    }

    private void handleCameraOption() {
        final File imageFile = imageUtil.createFile(false);
        if (imageFile == null) {
            return;
        }
        saveSource(CAMERA);
        final Uri takePhotoUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
            takePhotoUri = FileProvider.getUriForFile(fragment.getActivity(),
                                                      fragment.getActivity().getApplicationContext().getPackageName() + ".provider",
                                                      imageFile);
        } else {
            takePhotoUri = Uri.fromFile(imageFile);
        }
        saveUri(takePhotoUri);
        Fetch.imageFromCamera().to(fragment, takePhotoUri);
    }

    private void handleGalleryOption() {
        if (permission.isGranted()) {
            saveSource(GALLERY);
            Fetch.imageFromGallery().to(fragment);
        } else {
            permission.requestPermission();
        }
    }

    private void handleRemoveOption() {
        clear();
        getListener().onRemove();
    }

    //endregion

    private void saveUri(@NonNull final Uri uri) {
        preferences.edit()
                   .putString(PHOTO_URI_KEY, uri.toString())
                   .apply();
    }

    @NonNull
    private Uri getUri() {
        final String photoUriString = preferences.getString(PHOTO_URI_KEY, "");
        return photoUriString.isEmpty() ? Uri.EMPTY : Uri.parse(photoUriString);
    }

    public void saveSource(@NonNull final Source source) {
        preferences.edit()
                   .putString(PHOTO_SOURCE_KEY, source.name())
                   .apply();
    }

    @NonNull
    private Source getSource() {
        final String value = preferences.getString(PHOTO_SOURCE_KEY, "");
        return Source.fromString(value);
    }

    public void compressImage(@Nullable final Uri imageUri) {
        compressImageObservable(imageUri)
                .subscribe(this::compressImageSuccess,
                           this::compressImageError);
    }

    private void compressImageSuccess(@NonNull final TypedFile compressedFile) {
        getListener().onImageCompressedSuccess(compressedFile, getSource());

    }

    private void compressImageError(@NonNull final Throwable e) {
        setShowOptions(true);
        getListener().onImageCompressedError(e, R.string.error_account_upload_photo_title, R.string.error_account_upload_photo_message);
    }

    private Listener getListener() {
        return ((Listener) fragment);
    }

    public interface Listener {

        void onImportFromFacebook();

        void onFromCamera(@NonNull final Uri imageUri);

        void onFromGallery(@NonNull final Uri imageUri);

        void onRemove();

        void onImageCompressedSuccess(@NonNull TypedFile compressedImage, @NonNull Analytics.ProfilePhoto.Source source);

        void onImageCompressedError(@NonNull Throwable e, @StringRes int titleRes, @StringRes int messageRes);

    }

    public static class Builder {

        private final ImageUtil imageUtil;
        private final FilePathUtil filePathUtil;

        public Builder(@NonNull final ImageUtil imageUtil, @NonNull final FilePathUtil filePathUtil) {
            this.imageUtil = imageUtil;
            this.filePathUtil = filePathUtil;
        }

        public ProfileImageManager build(@NonNull final Fragment listener) throws NullPointerException {
            checkFragmentInstance(listener);
            return new ProfileImageManager(listener, imageUtil, filePathUtil);
        }

        private void checkFragmentInstance(@NonNull final Fragment fragment) {
            if (!(fragment instanceof Listener)) {
                throw new ClassCastException(
                        fragment.toString() + " must implement " + Listener.class.getSimpleName()
                );
            }
        }
    }

}
