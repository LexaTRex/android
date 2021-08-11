package de.culture4life.luca.ui.splash;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.BaseActivity;
import de.culture4life.luca.ui.MainActivity;
import de.culture4life.luca.ui.onboarding.OnboardingActivity;
import de.culture4life.luca.ui.registration.RegistrationActivity;
import de.culture4life.luca.ui.terms.UpdatedTermsActivity;
import de.culture4life.luca.ui.terms.UpdatedTermsUtil;
import timber.log.Timber;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Luca_DayNight);
        super.onCreate(savedInstanceState);
        hideActionBar();

        // check for app link data
        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null) {
                application.handleDeepLink(intent.getData())
                        .blockingAwait();
            }
        }

        if (!hasSeenWelcomeScreenBefore()) {
            navigate(OnboardingActivity.class);
        } else if (!hasCompletedRegistration()) {
            navigate(RegistrationActivity.class);
        } else {
            activityDisposable.add(UpdatedTermsUtil.Companion.areTermsAccepted(application).subscribe(accepted -> {
                if (accepted) {
                    navigate(MainActivity.class);
                } else {
                    navigate(UpdatedTermsActivity.class);
                }
            }));
        }
    }

    private void navigate(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private boolean hasSeenWelcomeScreenBefore() {
        return application.getPreferencesManager()
                .restoreOrDefault(OnboardingActivity.WELCOME_SCREEN_SEEN_KEY, false)
                .doOnError(throwable -> Timber.w("Unable to check if welcome screen has been seen before: %s", throwable.toString()))
                .onErrorReturnItem(false)
                .blockingGet();
    }

    private boolean hasCompletedRegistration() {
        return application.getRegistrationManager()
                .hasCompletedRegistration()
                .onErrorReturnItem(false)
                .blockingGet();
    }

}