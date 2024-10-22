package com.instant.mvi.view.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserInput(
    name: String,
    email: String,
    nameError: Boolean,
    emailError: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddUser: (Triple<String, String, Bitmap?>) -> Unit,  // Using Bitmap instead of Uri
    onClearUsers: () -> Unit
) {
    val imageBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // Input validation flags
    val isNameValid = remember(name) { name.isNotBlank() }
    val isEmailValid = remember(email) {
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Camera and gallery launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // Load the image from the gallery and convert to Bitmap
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)  // Helper function to convert Uri to Bitmap
            imageBitmap.value = bitmap
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Capture the image from the camera as a Bitmap
        imageBitmap.value = bitmap
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Select from Gallery")
            }

            Button(onClick = { cameraLauncher.launch(null) }) {
                Text("Take a Photo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conditional Modifier: Apply different modifiers based on whether an image is selected
        val imageModifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .border(2.dp, Color.Blue, CircleShape)

        // Display selected image as a circle or default gray circle
        imageBitmap.value?.let {
            Image(
                bitmap = it.asImageBitmap(),  // Use the Bitmap directly
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        } ?: Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.Gray, CircleShape)
                .border(2.dp, Color.Blue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("No Image", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            isError = nameError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            isError = emailError,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isNameValid && isEmailValid) {
                    onAddUser(Triple(name, email, imageBitmap.value))
                    imageBitmap.value = null // Clear the image after adding the user
                }
            },
            enabled = isNameValid && isEmailValid,  // Disable the button if there are validation errors
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add User")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onClearUsers,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Users")
        }
    }
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
    val filename = generateFilename()
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File(directory, "$filename.jpg")

    return try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
        }
        Uri.fromFile(file)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun generateFilename(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return "IMG_$timestamp"
}

