package com.duifenyi.app.data.model

/**
 * 登录状态
 */
sealed class LoginState {
    data object Idle : LoginState()
    data object LoggedOut : LoginState()
    data object LoggingIn : LoginState()
    data object LoggedIn : LoginState()
    data class Error(val message: String) : LoginState()
}
