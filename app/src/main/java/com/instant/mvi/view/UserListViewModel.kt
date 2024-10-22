package com.instant.mvi.view

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instant.mvi.model.User
import com.instant.mvi.repository.UserRepository
import com.instant.mvi.repository.UserRepositoryImpl
import com.instant.mvi.view.uimodel.SnackbarEffect
import com.instant.mvi.view.uimodel.UserIntent
import com.instant.mvi.view.uimodel.UserViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val repository: UserRepository,
    @ApplicationContext private val context: Context // Inject the Application Context
) : ViewModel() {

    private val _viewState = MutableStateFlow(UserViewState())
    val viewState: StateFlow<UserViewState> = _viewState

    // Effect channel to handle snackbar events
    private val _effectChannel = Channel<SnackbarEffect>()
    val effectFlow: Flow<SnackbarEffect> = _effectChannel.receiveAsFlow()

    private var recentlyDeletedUser: User? = null

    init {
        handleIntent(UserIntent.LoadUsers)
    }

    // Handle intents, including handling bitmap images
    fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.AddUser -> validateAndAddUser(intent.name, intent.email, intent.bitmap) // Pass Bitmap
            is UserIntent.DeleteUser -> deleteUser(intent.user)
            is UserIntent.ClearUsers -> clearUsers()
            is UserIntent.SearchUser -> searchUsers(intent.query)
            is UserIntent.UpdateName -> validateName(intent.name)
            is UserIntent.UpdateEmail -> validateEmail(intent.email)
            is UserIntent.UndoDelete -> undoDelete()
            is UserIntent.SelectImageFromGallery -> selectImageFromGallery(intent.uri)
            is UserIntent.CaptureImage -> captureImage(intent.bitmap)
        }
    }

    // Function to validate inputs in real-time
    private fun validateInput(name: String, email: String) {
        val nameError = name.isBlank()
        val emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        _viewState.value = _viewState.value.copy(
            name = name,
            email = email,
            nameError = nameError,
            emailError = emailError
        )
    }
    private fun validateAndAddUser(name: String, email: String, bitmap: Bitmap?) {
        val nameError = name.isBlank()
        val emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        if (nameError || emailError) {
            val errorMessage = when {
                nameError && emailError -> "Name and email are invalid."
                nameError -> "Name cannot be empty."
                emailError -> "Invalid email address."
                else -> ""
            }
            _viewState.value = _viewState.value.copy(
                nameError = nameError,
                emailError = emailError
            )
            viewModelScope.launch {
                _effectChannel.send(SnackbarEffect.ShowSnackbar(errorMessage))
            }
            return
        }

        // Check if the image is provided, and show a snackbar message if not
        if (bitmap == null) {
            viewModelScope.launch {
                _effectChannel.send(SnackbarEffect.ShowSnackbar("Please select or capture an image!"))
            }
            return
        }

        _viewState.value = _viewState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                // Convert Bitmap to file if it exists
                val imageUrl = saveBitmapToFile(bitmap)

                val newUser = User(id = (0..1000).random(), name = name, email = email, imageUrl = imageUrl)
                val users = repository.addUser(newUser)
                _viewState.value = _viewState.value.copy(
                    isLoading = false,
                    users = users,
                    name = "",
                    email = "",
                    selectedImageUri = null,
                    nameError = false,
                    emailError = false
                )
                _effectChannel.send(SnackbarEffect.ShowSnackbar("User added successfully!"))
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(isLoading = false)
                _effectChannel.send(SnackbarEffect.ShowSnackbar("Error adding user: ${e.message}"))
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            val users = repository.getUsers()
            _viewState.value = _viewState.value.copy(users = users)
        }
    }

    private fun deleteUser(user: User) {
        _viewState.value = _viewState.value.copy(isLoading = true)
        viewModelScope.launch {
            recentlyDeletedUser = user
            try {
                val users = repository.deleteUser(user)
                _viewState.value = _viewState.value.copy(isLoading = false, users = users)
                _effectChannel.send(SnackbarEffect.ShowSnackbar("User deleted", "Undo"))
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(isLoading = false)
                _effectChannel.send(SnackbarEffect.ShowSnackbar("Error deleting user: ${e.message}"))
            }
        }
    }

    private fun undoDelete() {
        recentlyDeletedUser?.let { deletedUser ->
            validateAndAddUser(deletedUser.name, deletedUser.email, null) // Don't need to restore image
            recentlyDeletedUser = null
        }
    }

    private fun clearUsers() {
        _viewState.value = _viewState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val users = repository.clearUsers()
                _viewState.value = _viewState.value.copy(isLoading = false, users = users)
                _effectChannel.send(SnackbarEffect.ShowSnackbar("All users cleared!"))
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(isLoading = false)
                _effectChannel.send(SnackbarEffect.ShowSnackbar("Error clearing users: ${e.message}"))
            }
        }
    }

    private fun searchUsers(query: String) {
        _viewState.value = _viewState.value.copy(searchQuery = query)
        viewModelScope.launch {
            val filteredUsers = _viewState.value.users.filter {
                it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
            _viewState.value = _viewState.value.copy(users = filteredUsers)
            if (filteredUsers.isEmpty()) {
                _effectChannel.send(SnackbarEffect.ShowSnackbar("No users found"))
            }
        }
    }

    // Function to validate only the name
    private fun validateName(name: String) {
        val nameError = name.isBlank()
        _viewState.value = _viewState.value.copy(
            name = name,
            nameError = nameError
        )
//        if (nameError) {
//            viewModelScope.launch {
//                _effectChannel.send(SnackbarEffect.ShowSnackbar("Name cannot be empty"))
//            }
//        }
    }

    // Function to validate only the email
    private fun validateEmail(email: String) {
        val emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        _viewState.value = _viewState.value.copy(
            email = email,
            emailError = emailError
        )
//        if (emailError) {
//            viewModelScope.launch {
//                _effectChannel.send(
//                    SnackbarEffect.ShowSnackbar(
//                        if (email.isEmpty()) "Email cannot be empty" else "Invalid email address"
//                    )
//                )
//            }
//        }
    }

    // Handle gallery image selection
    private fun selectImageFromGallery(uri: Uri) {
        _viewState.value = _viewState.value.copy(selectedImageUri = uri.toString())
        viewModelScope.launch {
            _effectChannel.send(SnackbarEffect.ShowSnackbar("Image selected from gallery!"))
        }
    }

    // Handle image capture from the camera
    private fun captureImage(bitmap: Bitmap) {
        val imageUri = saveBitmapToFile(bitmap) // Save the captured bitmap to file
        _viewState.value = _viewState.value.copy(selectedImageUri = imageUri)
        viewModelScope.launch {
            _effectChannel.send(SnackbarEffect.ShowSnackbar("Image captured successfully!"))
        }
    }

    // Helper method to save bitmap to file and return its URI
    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val file = File(context.cacheDir, "user_image_${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
