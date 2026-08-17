package com.cryptowallet.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cryptowallet.ui.theme.AuthBackgroundBottom
import com.cryptowallet.ui.theme.AuthBackgroundGlow
import com.cryptowallet.ui.theme.AuthBackgroundMid
import com.cryptowallet.ui.theme.AuthBackgroundTop
import com.cryptowallet.ui.theme.AuthFieldBorder
import com.cryptowallet.ui.theme.AuthLabelMuted
import com.cryptowallet.ui.theme.CryptoOrange
import com.cryptowallet.view.component.AppBackground2
import com.cryptowallet.view.component.AppButton
import com.cryptowallet.view.component.AppPageTitle
import com.cryptowallet.viewmodel.AuthMode
import com.cryptowallet.viewmodel.AuthUiState
import com.cryptowallet.viewmodel.AuthViewModel

private val AuthTextFieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = CryptoOrange,
        unfocusedBorderColor = AuthFieldBorder,
        focusedLabelColor = CryptoOrange,
        unfocusedLabelColor = AuthLabelMuted,
        cursorColor = CryptoOrange,
        focusedTrailingIconColor = AuthLabelMuted,
        unfocusedTrailingIconColor = AuthLabelMuted,
    )

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val isRegister = uiState.mode == AuthMode.REGISTER

    AppBackground2 {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 40.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            AppPageTitle(if (isRegister) "Create account" else "Sign in")
            Spacer(modifier = Modifier.height(40.dp))

            if (isRegister) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    colors = AuthTextFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = AuthTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = AuthLabelMuted,
                        )
                    }
                },
                colors = AuthTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isRegister) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = null,
                                tint = AuthLabelMuted,
                            )
                        }
                    },
                    colors = AuthTextFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            AppButton(
                text = if (isRegister) "Sign up" else "Sign in",
                onClick = viewModel::submit,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (isRegister) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuthLabelMuted,
                )
                Text(
                    text = if (isRegister) "Sign in" else "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CryptoOrange,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) { viewModel.toggleMode() },
                )
            }
        }
    }
}