package com.spider.videoandaudiocall.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import com.google.firebase.firestore.FirebaseFirestore;
import com.spider.videoandaudiocall.databinding.ActivitySignUpsBinding;
import com.spider.videoandaudiocall.utilites.BaseActivity;
import com.spider.videoandaudiocall.utilites.Constants;
import com.spider.videoandaudiocall.utilites.PreferenceManager;

import java.util.HashMap;
import java.util.Objects;

public class SignUpActivity extends BaseActivity {

    ActivitySignUpsBinding signUpBinding;
    private final Context context = SignUpActivity.this;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signUpBinding = ActivitySignUpsBinding.inflate(getLayoutInflater());
        setContentView(signUpBinding.getRoot());
        preferenceManager = new PreferenceManager(context);
        signUpBinding.textSignIn.setOnClickListener(view -> startActivity(new Intent(context, SignInActivity.class)));
        signUpBinding.imgBack.setOnClickListener(view -> onBackPressed());
        signUpBinding.buttonSignUp.setOnClickListener(view -> {
            if (Objects.requireNonNull(signUpBinding.inputFirstName.getText()).toString().trim().isEmpty()) {
                toast(Constants.first_name, context);
            } else if (Objects.requireNonNull(signUpBinding.inputLastName.getText()).toString().trim().isEmpty()) {
                toast(Constants.last_name, context);
            } else if (Objects.requireNonNull(signUpBinding.inputEmail.getText()).toString().trim().isEmpty()) {
                toast(Constants.Enter_email, context);
            } else if (!Patterns.EMAIL_ADDRESS.matcher(signUpBinding.inputEmail.getText().toString()).matches()) {
                toast(Constants.valid_email, context);
            } else if (Objects.requireNonNull(signUpBinding.inputPassword.getText()).toString().trim().isEmpty()) {
                toast(Constants.enter_password, context);
            } else if (Objects.requireNonNull(signUpBinding.inputConfirmPassword.getText()).toString().trim().isEmpty()) {
                toast(Constants.comfirm_password, context);
            } else if (!signUpBinding.inputPassword.getText().toString().equals(signUpBinding.inputConfirmPassword.getText().toString())) {
                toast(Constants.same_password, context);
            } else {
                signUp();
            }
        });
    }

    private void signUp() {
        signUpBinding.buttonSignUp.setVisibility(View.INVISIBLE);
        signUpBinding.progressBarSignUp.setVisibility(View.VISIBLE);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> users = new HashMap<>();
        users.put(Constants.KEY_FIRST_NAME, Objects.requireNonNull(signUpBinding.inputFirstName.getText()).toString());
        users.put(Constants.KEY_LAST_NAME, Objects.requireNonNull(signUpBinding.inputLastName.getText()).toString());
        users.put(Constants.KEY_EMAIL, Objects.requireNonNull(signUpBinding.inputEmail.getText()).toString());
        users.put(Constants.KEY_PASSWORD, Objects.requireNonNull(signUpBinding.inputPassword.getText()).toString());
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(users)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, signUpBinding.inputFirstName.getText().toString());
                    preferenceManager.putString(Constants.KEY_LAST_NAME, signUpBinding.inputLastName.getText().toString());
                    preferenceManager.putString(Constants.KEY_EMAIL, signUpBinding.inputEmail.getText().toString());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    signUpBinding.buttonSignUp.setVisibility(View.VISIBLE);
                    signUpBinding.progressBarSignUp.setVisibility(View.INVISIBLE);
                    toast(Constants.error_msg, context);
                });
    }
}