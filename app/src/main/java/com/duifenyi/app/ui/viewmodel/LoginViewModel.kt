package com.duifenyi.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duifenyi.app.data.local.PreferencesManager
import com.duifenyi.app.data.model.LoginState
import com.duifenyi.app.data.repository.DuifenyiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    val repository = DuifenyiRepository(prefs)

    private val _showPasteDialog = MutableStateFlow(false)
    val showPasteDialog: StateFlow<Boolean> = _showPasteDialog.asStateFlow()

    private val _authUrl = MutableStateFlow("")
    val authUrl: StateFlow<String> = _authUrl.asStateFlow()

    init {
        viewModelScope.launch {
            repository.init()

            // 如果已登录，直接跳过登录界面
            if (repository.loginState.value == LoginState.LoggedIn) {
                // 由导航处理
            }
        }
    }

    fun getWeChatAuthUrl(): String {
        // 与Python版完全相同的微信授权URL
        val url = "https://open.weixin.qq.com/connect/oauth2/authorize" +
                "?appid=wx1b5650884f657981" +
                "&redirect_uri=https://www.duifene.com/_FileManage/PdfView.aspx" +
                "?file=https%3A%2F%2Ffs.duifene.com%2Fres%2Fr2%2Fu6106199%2F" +
                "%E5%AF%B9%E5%88%86%E6%98%93%E7%99%BB%E5%BD%95_876c9d439ca68ead389c.pdf" +
                "&response_type=code&scope=snsapi_userinfo&connect_redirect=1#wechat_redirect"
        _authUrl.value = url
        return url
    }

    fun loginWithCode(code: String) {
        viewModelScope.launch {
            repository.loginWithWeChatCode(code)
        }
    }

    fun togglePasteDialog() {
        _showPasteDialog.value = !_showPasteDialog.value
    }
}
