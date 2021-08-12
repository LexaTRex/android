package de.culture4life.luca.ui.onboarding;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import de.culture4life.luca.R;
import de.culture4life.luca.ui.BaseActivity;
import de.culture4life.luca.ui.registration.RegistrationActivity;
import de.culture4life.luca.ui.terms.UpdatedTermsUtil;

public class OnboardingActivity extends BaseActivity {

    public static final String WELCOME_SCREEN_SEEN_KEY = "welcome_screen_seen";

    private MaterialButton primaryActionButton;
    private MaterialCheckBox termsCheckBox;
    private TextView termsTextView;
    private TextView privacyTextView;
    private Drawable errorDrawable;
    private int errorTint;
    private int normalTint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showWelcomeScreen();
        hideActionBar();
    }

    private void showWelcomeScreen() {
        setContentView(R.layout.fragment_onboarding_welcome);

        primaryActionButton = findViewById(R.id.primaryActionButton);
        primaryActionButton.setOnClickListener(view -> {
            if (termsCheckBox.isChecked()) {
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
            if (termsCheckBox.isChecked()) {
                setErrorFor(termsCheckBox, termsTextView, false);
            }
        };

        termsCheckBox = findViewById(R.id.termsCheckBox);
        termsCheckBox.setOnCheckedChangeListener(checkBoxListener);

        termsTextView = findViewById(R.id.termsTextView);
        termsTextView.setMovementMethod(LinkMovementMethod.getInstance());

        privacyTextView = findViewById(R.id.privacyTextView);
        privacyTextView.setMovementMethod(LinkMovementMethod.getInstance());

        errorTint = getResources().getColor(R.color.material_red_300);
        normalTint = termsCheckBox.getButtonTintList().getDefaultColor();
        errorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_error_outline);
        errorDrawable.setTint(errorTint);
    }

    private void setErrorFor(MaterialCheckBox checkBox, TextView textView, boolean hasError) {
        checkBox.setButtonTintList(ColorStateList.valueOf(hasError ? errorTint : normalTint));
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, hasError ? errorDrawable : null, null);
    }

    private void showCheckboxErrors() {
        setErrorFor(termsCheckBox, termsTextView, !termsCheckBox.isChecked());
    }

    private void showInfoScreen() {
        setContentView(R.layout.fragment_onboarding_info);
        primaryActionButton = findViewById(R.id.primaryActionButton);
        primaryActionButton.setOnClickListener(view -> showRegistration());
    }

    private void showRegistration() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}