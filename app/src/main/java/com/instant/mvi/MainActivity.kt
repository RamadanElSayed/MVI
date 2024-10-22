package com.instant.mvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.instant.mvi.view.UserListViewModel
import com.instant.mvi.view.components.UserInput
import com.instant.mvi.view.components.UserItem
import com.instant.mvi.view.uimodel.SnackbarEffect
import com.instant.mvi.view.uimodel.UserIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint  // Ensure Hilt is aware of this activity
class MainActivity : ComponentActivity() {
    private val userListViewModel: UserListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(userListViewModel = userListViewModel)
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyApp(userListViewModel: UserListViewModel) {
        val viewState by userListViewModel.viewState.collectAsState()
        val listState = rememberLazyListState()

        // State to manage Snackbar
        val snackbarHostState = remember { SnackbarHostState() }

        // Collect snackbar events and show snackbar
        LaunchedEffect(userListViewModel) {
            userListViewModel.effectFlow.collectLatest { effect ->
                when (effect) {
                    is SnackbarEffect.ShowSnackbar -> {
                        val result = snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = effect.actionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed && effect.actionLabel == "Undo") {
                            userListViewModel.handleIntent(UserIntent.UndoDelete)
                        }
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("User Management") })
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }, // Attach the SnackbarHost
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(viewState.users) { user ->
                            UserItem(
                                user = user,
                                onDeleteUser = {
                                    userListViewModel.handleIntent(
                                        UserIntent.DeleteUser(
                                            it
                                        )
                                    )
                                })
                        }
                    }

                    UserInput(
                        name = viewState.name,
                        email = viewState.email,
                        nameError = viewState.nameError,
                        emailError = viewState.emailError,
                        onNameChange = { userListViewModel.handleIntent(UserIntent.UpdateName(it)) },
                        onEmailChange = { userListViewModel.handleIntent(UserIntent.UpdateEmail(it)) },
                        onAddUser = {
                            userListViewModel.handleIntent(
                                UserIntent.AddUser(
                                    it.first,
                                    it.second,
                                    it.third
                                )
                            )
                        },
                        onClearUsers = { userListViewModel.handleIntent(UserIntent.ClearUsers) }
                    )
                }
            }
        )
    }

}




