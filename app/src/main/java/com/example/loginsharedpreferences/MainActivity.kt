package com.example.loginsharedpreferences

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.loginsharedpreferences.ui.theme.LoginSharedPreferencesTheme

private const val PREFS_NAME = "auth_prefs"
private const val KEY_USERNAME = "username"
private const val KEY_PASSWORD = "password"
private const val KEY_IS_LOGGED_IN = "is_logged_in"
private const val DEFAULT_USERNAME = "aaa"
private const val DEFAULT_PASSWORD = "123"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoginSharedPreferencesTheme {
                LoginDemoApp()
            }
        }
    }
}

@Composable
private fun LoginDemoApp() {
    val context = LocalContext.current
    val authPreferences = remember(context) {
        AuthPreferences(context).apply {
            saveDemoCredentials()
        }
    }
    val savedCredentials = remember(authPreferences) {
        authPreferences.getSavedCredentials() ?: Credentials(DEFAULT_USERNAME, DEFAULT_PASSWORD)
    }
    var currentScreen by rememberSaveable {
        mutableStateOf(if (authPreferences.isLoggedIn()) AuthScreen.Success else AuthScreen.Login)
    }
    var successUsername by rememberSaveable {
        mutableStateOf(savedCredentials.username)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentScreen) {
            AuthScreen.Login -> {
                LoginScreen(
                    modifier = contentModifier,
                    initialUsername = savedCredentials.username,
                    initialPassword = savedCredentials.password,
                    onLogin = { username, password ->
                        if (username == savedCredentials.username && password == savedCredentials.password) {
                            authPreferences.setLoggedIn(true)
                            successUsername = username
                            currentScreen = AuthScreen.Success
                            null
                        } else {
                            "用户名或密码错误"
                        }
                    }
                )
            }

            AuthScreen.Success -> {
                SuccessScreen(
                    modifier = contentModifier,
                    username = successUsername,
                    onLogout = {
                        authPreferences.setLoggedIn(false)
                        currentScreen = AuthScreen.Login
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    modifier: Modifier = Modifier,
    initialUsername: String,
    initialPassword: String,
    onLogin: (String, String) -> String?
) {
    var username by rememberSaveable(initialUsername) { mutableStateOf(initialUsername) }
    var password by rememberSaveable(initialPassword) { mutableStateOf(initialPassword) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AuthCard(
        modifier = modifier,
        title = "登录",
        subtitle = "演示账号已保存在 SharedPreferences：aaa / 123"
    ) {
        CredentialFields(
            username = username,
            password = password,
            onUsernameChange = {
                username = it
                errorMessage = null
            },
            onPasswordChange = {
                password = it
                errorMessage = null
            }
        )

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val trimmedUsername = username.trim()
                errorMessage = when {
                    trimmedUsername.isEmpty() || password.isEmpty() -> "用户名和密码不能为空"
                    else -> onLogin(trimmedUsername, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "登录")
        }
    }
}

@Composable
private fun SuccessScreen(
    modifier: Modifier = Modifier,
    username: String,
    onLogout: () -> Unit
) {
    AuthCard(
        modifier = modifier,
        title = "登录成功",
        subtitle = "欢迎你，$username"
    ) {
        Text(text = "SharedPreferences 校验通过，当前已进入成功页面。")

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "退出登录")
        }
    }
}

@Composable
private fun AuthCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                content()
            }
        }
    }
}

@Composable
private fun CredentialFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text(text = "用户名") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(text = "密码") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

private data class Credentials(
    val username: String,
    val password: String
)

private enum class AuthScreen {
    Login,
    Success
}

private class AuthPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveDemoCredentials() {
        preferences.edit()
            .putString(KEY_USERNAME, DEFAULT_USERNAME)
            .putString(KEY_PASSWORD, DEFAULT_PASSWORD)
            .apply()
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply()
    }

    fun isLoggedIn(): Boolean = preferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getSavedCredentials(): Credentials? {
        val username = preferences.getString(KEY_USERNAME, null)
        val password = preferences.getString(KEY_PASSWORD, null)

        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            return null
        }

        return Credentials(username = username, password = password)
    }
}