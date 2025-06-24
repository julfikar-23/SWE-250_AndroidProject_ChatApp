package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.activities.utilities.Constants;
import com.example.chatapp.activities.utilities.PreferenceManager;
import com.example.chatapp.databinding.ActivitySigninBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SigninActivity extends AppCompatActivity {

    private ActivitySigninBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        binding = ActivitySigninBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(view -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
    }

    private void signIn() {
        loading(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(
                binding.inputEmail.getText().toString(),
                binding.inputPassword.getText().toString()
        ).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (auth.getCurrentUser().isEmailVerified()) {
                    loadUserDetails(auth.getCurrentUser().getUid());
                } else {
                    loading(false);
                    showToast("Please verify your email first.");
                    auth.signOut();
                }
            } else {
                loading(false);
                showToast("Sign in failed: " + task.getException().getMessage());
            }
        });
    }

    private void loadUserDetails(String userId) {
        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, userId);
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("User data not found.");
                    }
                });
    }

    private boolean isValidSignInDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
