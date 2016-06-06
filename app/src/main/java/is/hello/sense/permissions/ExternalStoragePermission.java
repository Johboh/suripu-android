package is.hello.sense.permissions;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.Manifest;
import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class ExternalStoragePermission extends Permission {

    public ExternalStoragePermission(@NonNull final Fragment fragment) {
        super(fragment);
    }

    public ExternalStoragePermission(@NonNull final Fragment fragment,
                                     @StringRes final int negativeText,
                                     @StringRes final int positiveText) {
        super(fragment, negativeText, positiveText);
    }

    public void requestPermissionWithDialogForCamera(){
        requestPermissionWithDialog(
                R.string.request_permission_write_external_storage_title,
                R.string.request_permission_write_external_storage_for_profile_picture,
                (dialog, which) -> UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity()));
    }

    public void showEnableInstructionsDialogForCamera(){
        showEnableInstructionsDialog(R.string.request_permission_write_external_storage_required_title,
                                     R.string.request_permission_write_external_storage_required_message,
                                     (dialog, which) -> UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity()));
    }

    public void requestPermissionWithDialogForGallery(){
        requestPermissionWithDialog(
                R.string.request_permission_write_external_storage_title,
                R.string.request_permission_write_external_storage_for_profile_picture,
                (dialog, which) -> UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity()));
    }

    @Override
    protected String getPermissionName() {
        return Manifest.permission.WRITE_EXTERNAL_STORAGE;
    }

    @Override
    public void requestPermissionWithDialog() {
        requestPermissionWithDialog(
                R.string.request_permission_write_external_storage_title,
                R.string.request_permission_write_external_storage_message,
                (dialog, which) -> UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity()));
    }

    @Override
    public void showEnableInstructionsDialog() {
        showEnableInstructionsDialog(R.string.request_permission_write_external_storage_required_title,
                                     R.string.request_permission_write_external_storage_required_message,
                                     (dialog, which) -> UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity()));
    }
}
