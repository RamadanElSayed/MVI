package com.instant.mvi.view.uimodel

import com.instant.mvi.model.User

// ViewState to manage the UI state
data class UserViewState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val name: String = "",
    val email: String = "",
    val nameError: Boolean = false,
    val emailError: Boolean = false,
    val searchQuery: String = "",
    val recentlyDeletedUser: User? = null,
    val selectedImageUri: String? = null
)
