package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.FacebookApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.v2.FacebookProfilePicture;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.adapter.AccountSettingsRecyclerAdapter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Fetch;
import is.hello.sense.util.ImageUtil;
import is.hello.sense.util.Logger;

public class AccountSettingsFragment extends InjectionFragment
        implements AccountEditor.Container {
    private static final int REQUEST_CODE_PASSWORD = 0x20;
    private static final int REQUEST_CODE_ERROR = 0xE3;
    private static final int REQUEST_CODE_PICTURE = 0x30;
    private static final int OPTION_ID_FROM_FACEBOOK = 0;
    private static final int OPTION_ID_FROM_CAMERA = 1;
    private static final int OPTION_ID_FROM_GALLERY = 2;
    private static final int OPTION_ID_REMOVE_PICTURE = 4;

    @Inject
    FacebookApiService facebookApiService;
    @Inject Picasso picasso;
    @Inject CallbackManager facebookCallbackManager;
    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject ImageUtil imageUtil;

    private ProgressBar loadingIndicator;


    private AccountSettingsRecyclerAdapter.CircleItem profilePictureItem;
    private SettingsRecyclerAdapter.DetailItem nameItem;
    private SettingsRecyclerAdapter.DetailItem emailItem;

    private SettingsRecyclerAdapter.DetailItem birthdayItem;
    private SettingsRecyclerAdapter.DetailItem genderItem;
    private SettingsRecyclerAdapter.DetailItem heightItem;
    private SettingsRecyclerAdapter.DetailItem weightItem;

    private SettingsRecyclerAdapter.ToggleItem enhancedAudioItem;

    private Account currentAccount;
    private @Nullable Account.Preferences accountPreferences;
    private RecyclerView recyclerView;
    private SettingsRecyclerAdapter adapter;

    private ExternalStoragePermission permission;
    private Uri imageUri;
    private Uri tempImageUri;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.currentAccount = (Account) savedInstanceState.getSerializable("currentAccount");
        } else {
            Analytics.trackEvent(Analytics.Backside.EVENT_ACCOUNT, null);
        }

        accountPresenter.update();
        addPresenter(accountPresenter);

        permission = ExternalStoragePermission.forCamera(this);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.static_recycler, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.static_recycler_view_loading);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.static_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     EnumSet.of(ScrollEdge.TOP), FadingEdgesItemDecoration.Style.STRAIGHT));

        this.adapter = new AccountSettingsRecyclerAdapter(getActivity(), picasso);

        final int verticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        final int sectionPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);

        decoration.addTopInset(adapter.getItemCount(), verticalPadding);

        this.profilePictureItem = new AccountSettingsRecyclerAdapter.CircleItem(this::changePicture);
        adapter.add(profilePictureItem);

        this.nameItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.missing_data_placeholder),
                                                               this::changeName);
        nameItem.setIcon(R.drawable.icon_settings_name, R.string.label_name);
        adapter.add(nameItem);

        this.emailItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.missing_data_placeholder),
                                                                this::changeEmail);
        emailItem.setIcon(R.drawable.icon_settings_email, R.string.label_email);
        adapter.add(emailItem);

        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        final SettingsRecyclerAdapter.DetailItem passwordItem =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.title_change_password),
                                                       this::changePassword);
        passwordItem.setIcon(R.drawable.icon_settings_lock, R.string.label_password);
        adapter.add(passwordItem);


        this.birthdayItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_dob),
                                                                   this::changeBirthDate);
        birthdayItem.setIcon(R.drawable.icon_settings_calendar, R.string.label_dob);
        adapter.add(birthdayItem);
        this.genderItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_gender),
                                                                 this::changeGender);
        genderItem.setIcon(R.drawable.icon_settings_gender, R.string.label_gender);
        adapter.add(genderItem);

        this.heightItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_height),
                                                                 this::changeHeight);
        heightItem.setIcon(R.drawable.icon_settings_height, R.string.label_height);
        adapter.add(heightItem);

        this.weightItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_weight),
                                                                 this::changeWeight);
        weightItem.setIcon(R.drawable.icon_settings_weight, R.string.label_weight);
        adapter.add(weightItem);

        decoration.addTopInset(adapter.getItemCount(), sectionPadding);
        this.enhancedAudioItem = new SettingsRecyclerAdapter.ToggleItem(getString(R.string.label_enhanced_audio),
                                                                        this::changeEnhancedAudio);
        adapter.add(enhancedAudioItem);

        adapter.add(new SettingsRecyclerAdapter.TextItem(getString(R.string.info_enhanced_audio), null));

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, sectionPadding, 0, verticalPadding));
        final SettingsRecyclerAdapter.DetailItem signOutItem =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.action_log_out),
                                                       this::signOut);
        signOutItem.setIcon(R.drawable.icon_settings_signout, R.string.action_log_out);
        adapter.add(signOutItem);

        recyclerView.setAdapter(adapter);

        loadingIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);

        handleFacebookCallbacks();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(accountPresenter.account,
                         this::bindAccount,
                         this::accountUnavailable);

        bindAndSubscribe(accountPresenter.preferences(),
                         this::bindAccountPreferences,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.loadingIndicator = null;

        this.nameItem = null;
        this.emailItem = null;

        this.birthdayItem = null;
        this.genderItem = null;
        this.heightItem = null;
        this.weightItem = null;

        this.enhancedAudioItem = null;

        this.recyclerView = null;
        this.adapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("currentAccount", currentAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Todo should we let facebook listen to all results?
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == REQUEST_CODE_PASSWORD) {
            accountPresenter.update();
        } else if (requestCode == REQUEST_CODE_ERROR) {
            getActivity().finish();
        } else if(requestCode == Fetch.Image.REQUEST_CODE_CAMERA) {
            setUri(this.tempImageUri);
            setTempImageUri(null);
            profilePictureItem.setValue(this.imageUri.toString());
        } else if(requestCode == Fetch.Image.REQUEST_CODE_GALLERY){
            final Uri imageUri = data.getData();
            profilePictureItem.setValue(imageUri.toString());
            setUri(imageUri);
        } else if(requestCode == REQUEST_CODE_PICTURE){
            final int optionID = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, -1);
            handlePictureOptionSelection(optionID);
        }
    }

    //endregion


    //region Glue

    public FragmentNavigationActivity getNavigationContainer() {
        return (FragmentNavigationActivity) getActivity();
    }

    private void showLoadingIndicator() {
        recyclerView.setVisibility(View.INVISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        recyclerView.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
    }

    //endregion


    //region Binding Data

    public void bindAccount(@NonNull Account account) {
        profilePictureItem.setValue(account.getProfilePictureUrl(getResources()));
        nameItem.setText(account.getFullName());
        emailItem.setText(account.getEmail());

        birthdayItem.setValue(dateFormatter.formatAsLocalizedDate(account.getBirthDate()));
        genderItem.setValue(getString(account.getGender().nameRes));

        final CharSequence weight = unitFormatter.formatWeight(account.getWeight());
        weightItem.setValue(weight.toString());

        final CharSequence height = unitFormatter.formatHeight(account.getHeight());
        heightItem.setValue(height.toString());

        this.currentAccount = account;

        hideLoadingIndicator();
    }

    public void accountUnavailable(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getResources()).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void bindAccountPreferences(@NonNull Account.Preferences preferences) {
        this.accountPreferences = preferences;
        enhancedAudioItem.setValue(preferences.enhancedAudioEnabled);
    }

    //endregion


    //region Basic Info
    private void changePicture() {
        if(permission.isGranted()){
            this.showPictureOptions();
        } else {
            permission.requestPermissionWithDialogForCamera();
        }
    }

    public void changeName() {
        final ChangeNameFragment fragment = new ChangeNameFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.action_change_name), true);
    }

    public void changeEmail() {
        final ChangeEmailFragment fragment = new ChangeEmailFragment();
        fragment.setTargetFragment(this, REQUEST_CODE_PASSWORD);
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_email), true);
    }

    public void changePassword() {
        final ChangePasswordFragment fragment = ChangePasswordFragment.newInstance(currentAccount.getEmail());
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_password), true);
    }

    //endregion


    //region Demographics

    public void changeBirthDate() {
        final OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_dob), true);
    }

    public void changeGender() {
        final OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_gender), true);
    }

    public void changeHeight() {
        final OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_height), true);
    }

    public void changeWeight() {
        final OnboardingRegisterWeightFragment fragment = new OnboardingRegisterWeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_weight), true);
    }

    //endregion


    //region Preferences

    public void changeEnhancedAudio() {
        if (accountPreferences == null) {
            return;
        }

        accountPreferences.enhancedAudioEnabled = !enhancedAudioItem.getValue();
        enhancedAudioItem.setValue(accountPreferences.enhancedAudioEnabled);

        showLoadingIndicator();
        bindAndSubscribe(accountPresenter.updatePreferences(accountPreferences),
                         ignored -> {
                             preferences.edit()
                                        .putBoolean(PreferencesPresenter.ENHANCED_AUDIO_ENABLED,
                                                    accountPreferences.enhancedAudioEnabled)
                                        .apply();
                             hideLoadingIndicator();
                         },
                         e -> {
                             accountPreferences.enhancedAudioEnabled = !accountPreferences.enhancedAudioEnabled;
                             enhancedAudioItem.setValue(accountPreferences.enhancedAudioEnabled);
                             accountUnavailable(e);
                         });
    }

    //endregion


    //region Actions

    public void signOut() {
        Analytics.trackEvent(Analytics.Backside.EVENT_SIGN_OUT, null);

        final SenseAlertDialog signOutDialog = new SenseAlertDialog(getActivity());
        signOutDialog.setTitle(R.string.dialog_title_log_out);
        signOutDialog.setMessage(R.string.dialog_message_log_out);
        signOutDialog.setNegativeButton(android.R.string.cancel, null);
        signOutDialog.setPositiveButton(R.string.action_log_out, (dialog, which) -> {
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_OUT, null);
            // Let the dialog finish dismissing before we block the main thread.
            recyclerView.post(() -> {
                getActivity().finish();
                accountPresenter.logOut();
            });
        });
        signOutDialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        signOutDialog.show();
    }

    //endregion


    //region Updates

    @NonNull
    @Override
    public Account getAccount() {
        return currentAccount;
    }

    @Override
    public void onAccountUpdated(@NonNull SenseFragment updatedBy) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(accountPresenter.saveAccount(currentAccount),
                             ignored -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 updatedBy.getFragmentManager().popBackStackImmediate();
                             },
                             e -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 ErrorDialogFragment.presentError(getActivity(), e);
                             });
        });
    }

    //endregion

    //region Camera Options

    private void handlePictureOptionSelection(final int optionID){
        switch(optionID){
            case OPTION_ID_FROM_FACEBOOK:
                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
                break;
            case OPTION_ID_FROM_CAMERA:
                File imageFile = imageUtil.createFile(true);
                if(imageFile != null){
                    Uri imageUri = Uri.fromFile(imageFile);
                    setTempImageUri(imageUri);
                    Fetch.imageFromCamera().fetch(this, imageUri);
                }
                break;
            case OPTION_ID_FROM_GALLERY:
                Fetch.imageFromGallery().fetch(this);
                break;
            case OPTION_ID_REMOVE_PICTURE:
                setUri(null);
                this.profilePictureItem.setValue(null);
            default:
                Logger.warn(AccountSettingsFragment.class.getSimpleName(),"unknown picture option selected");
        }
    }

    public void showPictureOptions() {
        //Todo Analytics.trackEvent(Analytics.Backside.EVENT_PICTURE_OPTIONS, null);

        ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
        // check if user has facebook app installed else open permissions
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_FACEBOOK)
                        .setTitle(R.string.action_import_from_facebook)
                        .setTitleColor(ContextCompat.getColor(getActivity(), R.color.text_dark))
                        .setIcon(R.drawable.settings_camera)
                   );
        if(imageUtil.hasDeviceCamera()){

            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_FROM_CAMERA)
                            .setTitle(R.string.action_take_photo)
                            .setTitleColor(ContextCompat.getColor(getActivity(), R.color.text_dark))
                            .setIcon(R.drawable.settings_camera)
                       );

        }

        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_GALLERY)
                .setTitle(R.string.action_import_from_gallery)
                .setTitleColor(ContextCompat.getColor(getActivity(), R.color.text_dark))
                .setIcon(R.drawable.settings_photo_library)
                );

        if(imageUri != null){
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_REMOVE_PICTURE)
                            .setTitle(R.string.action_remove_picture)
                            .setTitleColor(ContextCompat.getColor(getActivity(), R.color.destructive_accent))
                            .setIcon(R.drawable.icon_alarm_delete)
                       );
        }

        BottomSheetDialogFragment advancedOptions = BottomSheetDialogFragment.newInstance(options);
        advancedOptions.setTargetFragment(this, REQUEST_CODE_PICTURE);
        advancedOptions.showAllowingStateLoss(getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    public void setUri(Uri uri) {
        this.imageUri = uri;
    }

    public Uri getUri(){
        return this.imageUri;
    }
    //Used primarily for take picture from camera
    public Uri getTempImageUri() {
        return tempImageUri;
    }

    public void setTempImageUri(Uri tempImageUri) {
        this.tempImageUri = tempImageUri;
    }

    //endregion

    // region Facebook import

    private void handleFacebookCallbacks() {
        LoginManager.getInstance()
                    .registerCallback(
                            facebookCallbackManager,
                            new FacebookCallback<LoginResult>() {
                                @Override
                                public void onSuccess(LoginResult loginResult) {
                                    // App code
                                    AccessToken.setCurrentAccessToken(loginResult.getAccessToken());
                                    makeProfilePictureRequest("Bearer " + AccessToken.getCurrentAccessToken().getToken());
                                }

                                @Override
                                public void onCancel() {
                                    // App code
                                }

                                @Override
                                public void onError(FacebookException exception) {
                                    // App code
                                    Logger.debug(FacebookProfilePicture.class.getSimpleName(),"login failed", exception.fillInStackTrace());
                                }
                            });
    }

    private void makeProfilePictureRequest(String authToken){
        bindAndSubscribe(
                facebookApiService.getProfilePicture("0", "large", authToken),
                this::changePictureWithFacebook,
                this::handleFacebookError);
    }

    private void handleFacebookError(Throwable error) {
        Logger.debug(FacebookProfilePicture.class.getSimpleName(),"fetch profile picture failed", error);
    }

    private void changePictureWithFacebook(FacebookProfilePicture facebookProfilePicture) {
        final String fbImageUri = facebookProfilePicture.getImageUrl();
        if(fbImageUri != null){
            profilePictureItem.setValue(fbImageUri);
            setUri(Uri.parse(fbImageUri));
        }
    }

    // endregion

}
