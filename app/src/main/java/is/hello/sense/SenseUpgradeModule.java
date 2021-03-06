package is.hello.sense;


import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SwapSenseInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.hardware.SenseResetOriginalInteractor;
import is.hello.sense.interactors.pairsense.UpgradePairSenseInteractor;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.presenters.SenseResetOriginalPresenter;
import is.hello.sense.presenters.SenseUpgradeIntroPresenter;
import is.hello.sense.presenters.SenseUpgradeReadyPresenter;
import is.hello.sense.presenters.UnpairPillPresenter;
import is.hello.sense.presenters.UpgradePairSensePresenter;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.presenters.connectwifi.UpgradeConnectWifiPresenter;
import is.hello.sense.presenters.pairpill.BasePairPillPresenter;
import is.hello.sense.presenters.pairpill.UpgradePairPillPresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.UpgradeSelectWifiNetworkPresenter;
import is.hello.sense.ui.activities.SenseUpgradeActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.pill.PairPillFragment;
import is.hello.sense.ui.fragments.pill.UnpairPillFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.sense.SenseUpgradeIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpgradeReadyFragment;
import is.hello.sense.ui.fragments.updating.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.updating.SelectWifiNetworkFragment;

@Module(
        complete = false,
        includes = {
                //todo include after converting fragments to use presenters
                // SenseOTAModule.class,
        },
        injects = {
                SenseUpgradeActivity.class,
                SenseUpgradeIntroFragment.class,
                PairSenseFragment.class,
                SenseUpgradeReadyFragment.class,
                SenseResetOriginalFragment.class,
                UnpairPillFragment.class,
                PairPillFragment.class,
                BluetoothFragment.class,
                ConnectToWiFiFragment.class,
                SelectWifiNetworkFragment.class
        }
)
public class SenseUpgradeModule {

    //region Interactors

    @Provides
    @Singleton
    SwapSenseInteractor providesSwapSenseInteractor(final ApiService apiService) {
        return new SwapSenseInteractor(apiService);
    }

    @Provides
    @Singleton
    CurrentSenseInteractor providesCurrentSenseInteractor(final DevicesInteractor devicesInteractor) {
        return new CurrentSenseInteractor(devicesInteractor);
    }

    @Provides
    @Singleton
    UpgradePairSenseInteractor providesUpgradePairSenseInteractor(final HardwareInteractor hardwareInteractor,
                                                                  final SwapSenseInteractor swapSenseInteractor,
                                                                  final CurrentSenseInteractor resetOriginalInteractor) {
        return new UpgradePairSenseInteractor(hardwareInteractor, swapSenseInteractor, resetOriginalInteractor);
    }

    @Provides
    @Singleton
    SenseResetOriginalInteractor providesSenseResetOriginalInteractor(final Context context,
                                                                      final BluetoothStack bluetoothStack) {
        return new SenseResetOriginalInteractor(context, bluetoothStack);
    }

    @Provides
    @Singleton
    SenseUpgradeIntroPresenter providesSenseUpgradeIntroPresenter() {
        return new SenseUpgradeIntroPresenter();
    }

    //endregion

    //region Presenters

    @Provides
    @Singleton
    PairSensePresenter providesUpgradePairSensePresenter(@NonNull final HardwareInteractor interactor,
                                                         @NonNull final  DevicesInteractor devicesInteractor,
                                                         @NonNull final ApiService apiService,
                                                         @NonNull final UpgradePairSenseInteractor upgradePairSenseInteractor,
                                                         @NonNull final PreferencesInteractor preferencesInteractor) {
        return new UpgradePairSensePresenter(interactor, devicesInteractor, apiService, upgradePairSenseInteractor, preferencesInteractor);
    }

    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final  DevicesInteractor devicesInteractor,
                                                             @NonNull final ApiService apiService,
                                                             @NonNull final UpgradePairSenseInteractor pairSenseInteractor,
                                                             @NonNull final PreferencesInteractor preferencesInteractor) {
        return new UpgradeConnectWifiPresenter(hardwareInteractor, devicesInteractor, apiService, pairSenseInteractor, preferencesInteractor);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new UpgradeSelectWifiNetworkPresenter(interactor);
    }

    @Provides
    @Singleton
    SenseUpgradeReadyPresenter providesSenseUpgradeReadyPresenter() {
        return new SenseUpgradeReadyPresenter();
    }

    @Provides
    @Singleton
    BasePairPillPresenter providesUpdatePairPillPresenter(final HardwareInteractor interactor) {
        return new UpgradePairPillPresenter(interactor);
    }

    @Provides
    @Singleton
    UnpairPillPresenter providesUnpairPillPresenter(final HardwareInteractor hardwareInteractor,
                                                    final DevicesInteractor devicesInteractor) {
        return new UnpairPillPresenter(hardwareInteractor,
                                       devicesInteractor);
    }

    @Provides
    @Singleton
    SenseResetOriginalPresenter providesSenseResetOriginalPresenter(final SenseResetOriginalInteractor interactor,
                                                                    final CurrentSenseInteractor currentSenseInteractor) {
        return new SenseResetOriginalPresenter(interactor, currentSenseInteractor);
    }

    //endregion

}
