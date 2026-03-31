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

/*
 * 这个文件把整个 demo 都放在一起，方便新手从上到下阅读。
 * 阅读顺序建议如下：
 * 1. 先看 MainActivity，理解 Android 如何进入 Compose。
 * 2. 再看 LoginDemoApp，理解页面状态和页面切换。
 * 3. 然后看 LoginScreen / SuccessScreen，理解单个页面怎么写。
 * 4. 最后看 AuthPreferences，理解 SharedPreferences 是怎么存数据的。
 */

// SharedPreferences 文件名。
// Android 会用这个名字创建一份本地轻量级键值存储。
private const val PREFS_NAME = "auth_prefs"

// 下面三个 key 是 SharedPreferences 里使用的字段名。
// 可以把它们理解成 Map 里的键。
private const val KEY_USERNAME = "username"
private const val KEY_PASSWORD = "password"
private const val KEY_IS_LOGGED_IN = "is_logged_in"

// 演示账号。用户要求固定用 aaa / 123 登录。
private const val DEFAULT_USERNAME = "aaa"
private const val DEFAULT_PASSWORD = "123"

/**
 * MainActivity 是 Android 应用界面的入口 Activity。
 *
 * 在传统 View 写法里，这里通常会调用 setContentView 加载 XML。
 * 在 Compose 里，则是调用 setContent，把整个界面交给 Compose 去绘制。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 让内容可以延伸到状态栏、导航栏区域。
        // 这是现代 Android 界面常见的写法。
        enableEdgeToEdge()

        // setContent 是 Compose 的入口。
        // 大括号里的内容不是“立即画完就结束”，而是声明“界面应该长什么样”。
        setContent {
            // 外层套一层主题，颜色、字体、控件风格都会从这里继承。
            LoginSharedPreferencesTheme {
                // 这里是真正的业务页面根节点。
                LoginDemoApp()
            }
        }
    }
}

/**
 * 这是整个登录 demo 的根 Composable。
 *
 * 它负责三件事：
 * 1. 创建并持有 SharedPreferences 工具类。
 * 2. 维护当前应该显示“登录页”还是“成功页”。
 * 3. 把数据和事件传给具体页面。
 */
@Composable
private fun LoginDemoApp() {
    // LocalContext.current 可以拿到当前界面的 Android Context。
    // 在 Compose 里，如果想访问 SharedPreferences、Toast、资源等 Android 对象，
    // 通常都要先拿到 Context。
    val context = LocalContext.current

    // remember 的意思是：在当前这个 Composable 存活期间，记住这个对象，
    // 下次因为状态变化重新组合时不要重复创建。
    // 这里我们只希望 AuthPreferences 创建一次，所以用 remember 包起来。
    val authPreferences = remember(context) {
        AuthPreferences(context).apply {
            // 演示项目启动时预置固定账号，方便直接测试登录流程。
            // 这里每次进入页面都会执行，但写入的是同样的数据，所以问题不大。
            saveDemoCredentials()
        }
    }

    // 从 SharedPreferences 读取已经保存好的账号。
    // 如果读取失败，就退回到默认账号，保证 demo 一定可用。
    // 这里同样用 remember，是为了避免界面每次重组都去重复读取本地存储。
    val savedCredentials = remember(authPreferences) {
        authPreferences.getSavedCredentials() ?: Credentials(DEFAULT_USERNAME, DEFAULT_PASSWORD)
    }

    // currentScreen 表示当前应该显示哪个页面。
    // rememberSaveable 和 remember 很像，但它还能在旋转屏幕、配置变化时尽量保住值。
    // 这个状态是 Compose 页面切换的关键。
    var currentScreen by rememberSaveable {
        // 根据本地登录态决定是否直接进入成功页。
        mutableStateOf(if (authPreferences.isLoggedIn()) AuthScreen.Success else AuthScreen.Login)
    }

    // successUsername 用来在成功页面显示欢迎文案。
    // 登录成功后，会把当前用户名保存到这里。
    var successUsername by rememberSaveable {
        mutableStateOf(savedCredentials.username)
    }

    // Scaffold 是 Material 风格的页面骨架组件。
    // 它可以很方便地承载顶部栏、底部栏、浮动按钮、正文内容等。
    // 这个 demo 没有用到那么多功能，只把它当成一个标准页面容器使用。
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Modifier 是 Compose 里非常重要的概念。
        // 你可以把它理解成“给组件增加布局、尺寸、边距、点击等行为的链式配置”。
        // 这里的含义是：
        // 1. fillMaxSize(): 占满父布局
        // 2. padding(innerPadding): 避开 Scaffold 自动计算的安全区域内边距
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        // when 根据 currentScreen 的值决定当前显示哪一个页面。
        // 这就是 Compose 中最常见的“状态驱动界面”。
        // 不是手动跳转 XML 页面，而是状态变了，界面自然就变了。
        when (currentScreen) {
            AuthScreen.Login -> {
                LoginScreen(
                    modifier = contentModifier,
                    initialUsername = savedCredentials.username,
                    initialPassword = savedCredentials.password,
                    onLogin = { username, password ->
                        // 仅校验 SharedPreferences 中预置的演示账号。
                        // 如果用户名密码都匹配，就认为登录成功。
                        if (username == savedCredentials.username && password == savedCredentials.password) {
                            // 把“已登录”状态写入本地，便于下次进入时直接恢复成功页。
                            authPreferences.setLoggedIn(true)

                            // 把当前成功登录的用户名缓存到状态里，成功页会显示它。
                            successUsername = username

                            // 切换当前页面到成功页。
                            // 一旦这个状态改变，Compose 会自动重组并显示 SuccessScreen。
                            currentScreen = AuthScreen.Success

                            // 返回 null，表示没有错误信息。
                            null
                        } else {
                            // 返回错误文案，登录页会把它显示出来。
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
                        // 退出登录时，把本地登录状态清掉。
                        authPreferences.setLoggedIn(false)

                        // 然后把页面切回登录页。
                        currentScreen = AuthScreen.Login
                    }
                )
            }
        }
    }
}

/**
 * 登录页面。
 *
 * 参数说明：
 * 1. modifier: 外部传进来的布局修饰器。
 * 2. initialUsername / initialPassword: 输入框初始值。
 * 3. onLogin: 点击登录按钮后，由外部决定如何校验账号密码。
 *
 * 这样拆分的好处是：
 * 页面只关心“收集输入和展示结果”，
 * 真正的登录规则由外部根页面统一处理。
 */
@Composable
private fun LoginScreen(
    modifier: Modifier = Modifier,
    initialUsername: String,
    initialPassword: String,
    onLogin: (String, String) -> String?
) {
    // 这三个状态分别对应：用户名输入、密码输入、错误提示。
    // 只要它们变化，当前页面就会自动刷新到最新内容。
    var username by rememberSaveable(initialUsername) { mutableStateOf(initialUsername) }
    var password by rememberSaveable(initialPassword) { mutableStateOf(initialPassword) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // AuthCard 是我们自己封装的一个“通用卡片容器”。
    // 登录页和成功页都复用它，这样布局风格更统一。
    AuthCard(
        modifier = modifier,
        title = "登录",
        subtitle = "演示账号已保存在 SharedPreferences：aaa / 123"
    ) {
        // 把输入框抽成单独的 Composable，避免登录页函数过长。
        CredentialFields(
            username = username,
            password = password,
            onUsernameChange = {
                // 输入用户名时，先更新状态。
                username = it

                // 用户重新输入通常意味着正在修正错误，所以顺手把错误提示清掉。
                errorMessage = null
            },
            onPasswordChange = {
                password = it
                errorMessage = null
            }
        )

        // 只有 errorMessage 不为空时，才显示错误文本。
        // 这种根据条件决定是否显示某个组件，也是 Compose 很常见的写法。
        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                // trim() 会去掉用户名首尾空格，避免输入空格导致看不出来的问题。
                val trimmedUsername = username.trim()

                // 这里先做最基本的空值校验。
                // 如果不为空，再把输入交给外部 onLogin 继续判断是否匹配正确账号密码。
                errorMessage = when {
                    trimmedUsername.isEmpty() || password.isEmpty() -> "用户名和密码不能为空"
                    else -> onLogin(trimmedUsername, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Button 的内容本身也是 Compose 组件。
            // 所以按钮里直接放一个 Text 就能显示按钮文案。
            Text(text = "登录")
        }
    }
}

/**
 * 登录成功页面。
 *
 * 这个页面没有复杂逻辑，主要就是展示成功文案和一个退出按钮。
 */
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

/**
 * 通用卡片容器。
 *
 * 为什么要封装它：
 * 1. 登录页和成功页都有相似外观。
 * 2. 把公共布局抽出去后，具体页面只需要关注自己的内容。
 *
 * content 参数是一个“插槽”。
 * 调用方可以把任意 Compose 内容塞进这个卡片里。
 */
@Composable
private fun AuthCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    // Box 可以把子元素堆叠在同一层级上。
    // 这里主要用它做“居中容器”。
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Card 是 Material 风格卡片组件。
        // 常用于把一组相关内容装在一个有层次感的区域里。
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            // Column 会让里面的子元素按垂直方向依次摆放。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                // 每个子元素之间自动留 12.dp 间距。
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

/**
 * 用户名和密码输入框。
 *
 * 这个函数本身不保存状态。
 * 它采用的是“状态提升”写法：
 * 1. 当前值由外部传进来。
 * 2. 当用户输入变化时，通过回调把新值通知给外部。
 *
 * 这种方式在 Compose 里非常常见，因为更容易管理数据流。
 */
@Composable
private fun CredentialFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    // OutlinedTextField 是带边框的输入框。
    OutlinedTextField(
        // value 表示输入框当前显示什么内容。
        value = username,
        // onValueChange 表示用户每输入一个字符时，应该如何处理新值。
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
        // PasswordVisualTransformation 会把输入内容显示成圆点，避免明文可见。
        visualTransformation = PasswordVisualTransformation(),
        // 指定这是密码键盘类型，系统键盘会更贴合密码输入场景。
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

// data class 是 Kotlin 里专门用来装数据的类。
// 这里只有用户名和密码两个字段，所以非常适合用 data class。
private data class Credentials(
    val username: String,
    val password: String
)

// 这个枚举专门表示“当前页面是哪一个”。
// 只有两个值：登录页、成功页。
private enum class AuthScreen {
    Login,
    Success
}

/**
 * 对 SharedPreferences 的简单封装。
 *
 * 为什么要封装：
 * 1. 让页面代码不用直接接触 putString / getString 这些底层细节。
 * 2. 读写本地数据时，代码会更集中、更清晰。
 */
private class AuthPreferences(context: Context) {
    // applicationContext 生命周期更稳定，适合拿来做这类本地存储工具类。
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveDemoCredentials() {
        // 只负责写入演示账号，不处理登录状态。
        // edit() 表示开始编辑，putString() 表示写入字符串，apply() 表示异步提交。
        preferences.edit()
            .putString(KEY_USERNAME, DEFAULT_USERNAME)
            .putString(KEY_PASSWORD, DEFAULT_PASSWORD)
            .apply()
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        // 用单独的布尔值记录当前是否已经登录成功。
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply()
    }

    // 读取本地的登录状态。
    fun isLoggedIn(): Boolean = preferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getSavedCredentials(): Credentials? {
        // 从本地取出用户名和密码。
        val username = preferences.getString(KEY_USERNAME, null)
        val password = preferences.getString(KEY_PASSWORD, null)

        // 只要任意一个为空，就说明当前本地账号数据不完整。
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            return null
        }

        // 都不为空时，再组装成 Credentials 返回。
        return Credentials(username = username, password = password)
    }
}