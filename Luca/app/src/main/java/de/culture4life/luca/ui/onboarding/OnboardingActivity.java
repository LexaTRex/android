package de.culture4life.luca.ui.onboarding;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.checkbox.MaterialCheckBox;

import de.culture4life.luca.R;
import de.culture4life.luca.databinding.FragmentOnboardingInfoBinding;
import de.culture4life.luca.databinding.FragmentOnboardingWelcomeBinding;
import de.culture4life.luca.ui.BaseActivity;
import de.culture4life.luca.ui.registration.RegistrationActivity;
import de.culture4life.luca.ui.terms.UpdatedTermsUtil;

public class OnboardingActivity extends BaseActivity {

    public static final String WELCOME_SCREEN_SEEN_KEY = "welcome_screen_seen";

    private FragmentOnboardingWelcomeBinding welcomeBinding;
    private FragmentOnboardingInfoBinding infoBinding;
    private int errorTint;
    private int normalTint;
    private Drawable errorDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        welcomeBinding = FragmentOnboardingWelcomeBinding.inflate(getLayoutInflater());
        infoBinding = FragmentOnboardingInfoBinding.inflate(getLayoutInflater());

        showWelcomeScreen();
        hideActionBar();
    }

    private void showWelcomeScreen() {
        setContentView(welcomeBinding.getRoot());

        welcomeBinding.primaryActionButton.setOnClickListener(view -> {
            if (welcomeBinding.termsCheckBox.isChecked()) {
                activityDisposable.add(application.getPreferencesManager()
                        .persist(WELCOME_SCREEN_SEEN_KEY, true)
                        .andThen(UpdatedTermsUtil.Companion.markTermsAsAccepted(application))
                        .onErrorComplete()
                        .subscribe(this::showInfoScreen));
            } else {
                showCheckboxErrors();
            }
        });

        CompoundButton.OnCheckedChangeListener checkBoxListener = (view, isChecked) -> {
            if (welcomeBinding.termsCheckBox.isChecked()) {
                setErrorFor(welcomeBinding.termsCheckBox, welcomeBinding.termsTextView, false);
            }
        };

        welcomeBinding.termsCheckBox.setOnCheckedChangeListener(checkBoxListener);
        welcomeBinding.termsTextView.setMovementMethod(LinkMovementMethod.getInstance());
        welcomeBinding.privacyTextView.setMovementMethod(LinkMovementMethod.getInstance());

        errorTint = getResources().getColor(R.color.material_red_300);
        normalTint = welcomeBinding.termsCheckBox.getButtonTintList().getDefaultColor();
        errorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_error_outline);
        errorDrawable.setTint(errorTint);
    }

    private void setErrorFor(MaterialCheckBox checkBox, TextView textView, boolean hasError) {
        checkBox.setButtonTintList(ColorStateList.valueOf(hasError ? errorTint : normalTint));
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, hasError ? errorDrawable : null, null);
    }

    private void showCheckboxErrors() {
        setErrorFor(welcomeBinding.termsCheckBox, welcomeBinding.termsTextView, !welcomeBinding.termsCheckBox.isChecked());
    }

    private void showInfoScreen() {
        setContentView(infoBinding.getRoot());
        infoBinding.primaryActionButton.setOnClickListener(view -> showRegistration());
    }

    private void showRegistration() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}