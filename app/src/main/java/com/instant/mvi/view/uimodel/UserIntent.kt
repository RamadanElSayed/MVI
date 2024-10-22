package com.instant.mvi.view.uimodel

import android.graphics.Bitmap
import android.net.Uri
import com.instant.mvi.model.User


// Intent represents user interactions
// Intent represents user interactions
sealed class UserIntent {
    object LoadUsers : UserIntent()
    data class AddUser(val name: String, val email: String, val bitmap: Bitmap?) : UserIntent()
    data class DeleteUser(val user: User) : UserIntent()
    object ClearUsers : UserIntent()
    data class SearchUser(val query: String) : UserIntent()
    data class UpdateName(val name: String) : UserIntent()  // Intent for name changes
    data class UpdateEmail(val email: String) : UserIntent()  // Intent for email changes
    object UndoDelete : UserIntent()
    data class SelectImageFromGallery(val uri: Uri) : UserIntent()
    data class CaptureImage(val bitmap: Bitmap) : UserIntent()
}
