package com.spider.videoandaudiocall.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spider.videoandaudiocall.databinding.ActivitySignInBinding;
import com.spider.videoandaudiocall.utilites.BaseActivity;
import com.spider.videoandaudiocall.utilites.Constants;
import com.spider.videoandaudiocall.utilites.PreferenceManager;

import java.util.Objects;

public class SignInActivity extends BaseActivity {

    ActivitySignInBinding signInBinding;
    private final Context context = SignInActivity.this;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signInBinding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(signInBinding.getRoot());
        preferenceManager = new PreferenceManager(context);
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(context, MainActivity.class);
            startActivity(intent);
            finish();
        }
        signInBinding.textSignUp.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        signInBinding.buttonSignIn.setOnClickListener(view -> {
            if (Objects.requireNonNull(signInBinding.inputEmail.getText()).toString().trim().isEmpty()) {
                toast(Constants.Enter_email, context);
            } else if (!Patterns.EMAIL_ADDRESS.matcher(signInBinding.inputEmail.getText().toString()).matches()) {
                toast(Constants.valid_email, context);
            } else if (Objects.requireNonNull(signInBinding.inputPassword.getText()).toString().trim().isEmpty()) {
                toast(Constants.enter_password, context);
            } else {
                signIn();
            }
        });
    }

    private void signIn() {
        signInBinding.buttonSignIn.setVisibility(View.INVISIBLE);
        signInBinding.progressBarSignIn.setVisibility(View.VISIBLE);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, Objects.requireNonNull(signInBinding.inputEmail.getText()).toString())
                .whereEqualTo(Constants.KEY_PASSWORD, Objects.requireNonNull(signInBinding.inputPassword.getText()).toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        signInBinding.buttonSignIn.setVisibility(View.INVISIBLE);
                        signInBinding.progressBarSignIn.setVisibility(View.VISIBLE);
                        toast(Constants.sign_error_msg, context);
                    }
                });
    }
}