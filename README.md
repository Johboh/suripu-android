suripu-android
==============

It's Sense.app, for Android.

Prerequisites
=============

- [Java](http://support.apple.com/kb/DL1572) (on Yosemite).
- [Android Studio](http://developer.android.com/sdk/index.html).
- The [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) for lambda support.
- The correct SDK and build tools (now automatically installed by Android Studio.)
- The [Crashlytics Android Studio plugin](https://www.crashlytics.com/downloads/android-studio) for pushing builds.
- The key stores `Hello-Android-Internal.keystore` and `Hello-Android-Release.keystore`. Acquire these from another team member.

Building
========

Once all of the above prerequisites have been fulfilled, it should be possible to build Sense for Android by pressing the Run button in Android Studio.

Branching
=========

	master
		-> branch timeline-animations
		-> github pull request
		-> merge into master
	master
		-> tag 1.0.0rc2

The project currently has a single `master` branch, which should be considered semi-stable. All changes larger than 5 or so lines should ideally be made on branches made off `master`, later reviewed through the github pull request process. Stable builds should be archived using tags. The tag names should be of the form `major.minor.bugfix` and should have a monotonically increasing release candidate suffix. E.g. `1.0.0rc1`.

Bug fixes on stable code should be made by creating a new branch from the target tag, and then submitting a pull request into master. Once the code has been reviewed and tested, a build should be deployed from the branch, a tag should be created from the branch, and the branch should be merged into master and deleted.

Contributing
============

All code in the Sense for Android project is written in a restricted subset of Java 8 via the retrolambda back-compiler. It is safe to use all forms of Java 8 lambdas without restriction, with consideration for general memory usage by instances. __It is not__ possible to use interface default methods, or any of the JDK 8 classes and methods that have not been explicitly backported into the project. __Note:__ new code should not use anonymous inner classes for inline interface implementations unless a reference to `this` is required.

Code contributed to the project should more or less match the default "Reformat Code…" option in Android Studio. Generally this means opening braces on the same line, using spaces for indentation, and spaces after keywords on control structures. The project explicitly goes against the conventions in this formatter when it comes to @Attributes. Attributes are generally formatted to look like keywords. When in doubt, just run the reformat code option on your contribution.

All new code _without exception_ should use the `@NonNull` and `@Nullable` attributes for method parameters. Any fields in an object that can be `null` should be annotated with `@Nullable`. The project does not apply `@NonNull` in the same way.

Patterns
========

The majority of the project is written in the Model-View-Presenter pattern. All data flow happens through RxJava `Observable` objects, and all presenter state is held in `PresenterSubject` instances. __Important:__ the `PresenterSubject` class explicitly violates several interface guidelines from RxJava. A `PresenterSubject` never completes, and will silently emit new values after an error has occur. Any code that generically operates on `Observable` objects should take `PresenterSubject` into account until it can be replaced by something better. 

The project extensively uses dependency injection through `Dagger` to increase testable surface, and ease singleton creep. Convenience classes are provided that will perform dependency injection for you transparently. See `InjectionActivity`, `InjectionFragment`, `InjectionDialogFragment`, and `InjectionTestCase`. When using one of these classes, you only need to add your subclass to the appropriate module for `@Inject` fields to be satisifed.

Presenters and their views are loosely coupled through dependency injection. The general composition pattern is to use retained fragments for all major UI components, and to bind to the presenter's subjects in `onViewCreated`. The most common type of presenter, a single value presenter, can be created quickly by subclassing the `ValuePresenter` class. This will give you updating, low memory management, and state serialization for free.

Modules
=======

The project is broken into four modules:

- `ApiModule`: responsible for all communication with the backend. Satisfies all model and network related dependencies.
- `BluetoothModule`: responsible for all Bluetooth dependencies.
- `SenseAppModule`: responsible for all presenter dependencies.
- `TestModule`: an incomplete superset of `ApiModule`, `BluetoothModule`, and `SenseAppModule` that stubs out classes for tests.

Testing
=======

The project currently contains unit tests for most parts of the project with major logic. All of the presenters have accompanying synchronous unit tests, and most of the Bluetooth stack's non-radio related functionality is equipped. Any new presenters introduced into the project should have unit tests accompanying them when merged into `master`.
