package com.aj.udharbook.ui.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aj.udharbook.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    val context = LocalContext.current

    // ==================================================
    // FIREBASE AUTH
    // ==================================================

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // ==================================================
    // GOOGLE SIGN-IN CLIENT
    // ==================================================

    val googleSignInClient = remember {

        val webClientId =
            context.getString(
                R.string.default_web_client_id
            )

        Log.d(
            "AJ_LOGIN",
            "Web Client ID: $webClientId"
        )

        val options =
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

        GoogleSignIn.getClient(
            context,
            options
        )
    }

    // ==================================================
    // GOOGLE SIGN-IN RESULT
    // ==================================================

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            Log.d(
                "AJ_LOGIN",
                "Result Code = ${result.resultCode}"
            )

            try {

                val task =
                    GoogleSignIn.getSignedInAccountFromIntent(
                        result.data
                    )

                val account =
                    task.getResult(
                        ApiException::class.java
                    )

                Log.d(
                    "AJ_LOGIN",
                    "Google account = ${account.email}"
                )

                val idToken =
                    account.idToken

                // ==================================================
                // CHECK ID TOKEN
                // ==================================================

                if (idToken.isNullOrBlank()) {

                    loading = false

                    errorMessage =
                        "Google ID Token नहीं मिला।"

                    Log.e(
                        "AJ_LOGIN",
                        "ID TOKEN NULL"
                    )

                    return@rememberLauncherForActivityResult
                }

                // ==================================================
                // FIREBASE CREDENTIAL
                // ==================================================

                val credential =
                    GoogleAuthProvider.getCredential(
                        idToken,
                        null
                    )

                // ==================================================
                // FIREBASE AUTH
                // ==================================================

                auth.signInWithCredential(
                    credential
                )
                    .addOnCompleteListener { firebaseTask ->

                        loading = false

                        if (firebaseTask.isSuccessful) {

                            Log.d(
                                "AJ_LOGIN",
                                "FIREBASE LOGIN SUCCESS"
                            )

                            Log.d(
                                "AJ_LOGIN",
                                "Firebase User = ${
                                    auth.currentUser?.email
                                }"
                            )

                            errorMessage = null

                            onLoginSuccess()

                        } else {

                            val exception =
                                firebaseTask.exception

                            Log.e(
                                "AJ_LOGIN",
                                "FIREBASE LOGIN FAILED",
                                exception
                            )

                            errorMessage =
                                exception?.message
                                    ?: "Firebase Login failed"
                        }
                    }

            } catch (e: ApiException) {

                loading = false

                Log.e(
                    "AJ_LOGIN",
                    "GOOGLE API ERROR = ${e.statusCode}",
                    e
                )

                errorMessage =

                    when (e.statusCode) {

                        10 ->
                            "Developer Error (10): SHA-1 / OAuth configuration गलत है।"

                        12500 ->
                            "Google Sign-In failed (12500): SHA-1 या OAuth configuration check करें।"

                        12501 ->
                            "Google Sign-In cancel किया गया।"

                        12502 ->
                            "Google Sign-In अभी चल रहा है।"

                        7 ->
                            "Network connection उपलब्ध नहीं है।"

                        else ->
                            "Google Sign-In failed. Error code: ${e.statusCode}"
                    }

            } catch (e: Exception) {

                loading = false

                Log.e(
                    "AJ_LOGIN",
                    "GOOGLE SIGN-IN ERROR",
                    e
                )

                errorMessage =
                    e.message
                        ?: "Google Sign-In failed"
            }
        }

    // ==================================================
    // CHECK EXISTING LOGIN
    // ==================================================

    LaunchedEffect(Unit) {

        val currentUser =
            auth.currentUser

        if (currentUser != null) {

            Log.d(
                "AJ_LOGIN",
                "Already logged in: ${currentUser.email}"
            )

            onLoginSuccess()
        }
    }

    // ==================================================
    // UI
    // ==================================================

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        // ==================================================
        // APP NAME
        // ==================================================

        Text(
            text =
                "AJ Udhar Book",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        // ==================================================
        // LOGIN DESCRIPTION
        // ==================================================

        Text(
            text =
                "अपने Google Account से Sign In करें",

            modifier =
                Modifier.padding(
                    top = 8.dp
                )
        )

        // ==================================================
        // LOGIN BUTTON
        // ==================================================

        if (loading) {

            CircularProgressIndicator(

                modifier =
                    Modifier.padding(
                        24.dp
                    )
            )

        } else {

            Button(

                onClick = {

                    Log.d(
                        "AJ_LOGIN",
                        "LOGIN BUTTON CLICKED"
                    )

                    errorMessage = null

                    loading = true

                    try {

                        launcher.launch(

                            googleSignInClient
                                .signInIntent
                        )

                    } catch (e: Exception) {

                        loading = false

                        errorMessage =
                            e.message
                                ?: "Google Sign-In launch failed"

                        Log.e(
                            "AJ_LOGIN",
                            "LAUNCH ERROR",
                            e
                        )
                    }
                },

                modifier =
                    Modifier.padding(
                        top = 24.dp
                    )
            ) {

                Text(
                    text =
                        "Sign in with Google"
                )
            }
        }

        // ==================================================
        // ANOTHER ACCOUNT INFO
        // ==================================================

        Text(

            text =
                "दूसरे Google Account से login करने के लिए\n" +
                        "ऊपर वाले button पर दोबारा tap करें।",

            modifier =
                Modifier.padding(
                    top = 18.dp
                ),

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        // ==================================================
        // ERROR
        // ==================================================

        errorMessage?.let { message ->

            Text(

                text =
                    message,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                modifier =
                    Modifier.padding(
                        top = 16.dp
                    )
            )
        }
    }
}