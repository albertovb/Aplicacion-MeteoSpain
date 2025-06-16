package com.example.meteospain.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.meteospain.data.AppDatabase
import com.example.meteospain.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.meteospain.InstagramFontFamily
import com.example.meteospain.ui.theme.MeteoSpainTheme

@Composable
fun RegisterScreen(
    db: AppDatabase,
    onRegistered: (User) -> Unit,
    onLoginClick: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }
    var errorMessage   by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val lightOrange = Color(0xFFFFE0B2)
    val orange      = Color(0xFFFFB74D)
    val textFieldShape = RoundedCornerShape(8.dp)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = orange
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 42.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Título con fuente
                        Text(
                            text = "MeteoSpain",
                            fontSize = 40.sp,
                            fontFamily = InstagramFontFamily,
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(top = 22.dp)
                        )

                        Spacer(Modifier.height(30.dp))

                        Text(
                            text = "Registro",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                        // Campos
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Usuario") },
                            shape = textFieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo") },
                            shape = textFieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            shape = textFieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = repeatPassword,
                            onValueChange = { repeatPassword = it },
                            label = { Text("Repetir Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            shape = textFieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Botón Enviar
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.Main) {
                                    try {
                                        // 1. Validaciones previas
                                        if (username.isBlank() || email.isBlank() || password.isBlank() || repeatPassword.isBlank()) {
                                            errorMessage = "Por favor, rellena todos los campos"
                                            return@launch
                                        }
                                        if (username.length < 4) {
                                            errorMessage = "El usuario debe tener al menos 4 caracteres"
                                            return@launch
                                        }
                                        if (username.length > 15) {
                                            errorMessage = "El usuario no puede superar los 15 caracteres"
                                            return@launch
                                        }
                                        if (username.contains(' ')) {
                                            errorMessage = "El nombre de usuario no puede contener espacios"
                                            return@launch
                                        }
                                        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                            errorMessage = "Introduce un correo válido"
                                            return@launch
                                        }
                                        if (password.length < 5) {
                                            errorMessage = "La contraseña debe tener al menos 5 caracteres"
                                            return@launch
                                        }
                                        if (password != repeatPassword) {
                                            errorMessage = "Las contraseñas no coinciden"
                                            return@launch
                                        }

                                        val existsUser = withContext(Dispatchers.IO) {
                                            db.userDao().getByUsername(username.trim())
                                        }

                                        if (existsUser != null) {
                                            errorMessage = "El usuario ya está registrado"
                                            return@launch
                                        }

                                        val existsEmail = withContext(Dispatchers.IO) {
                                            db.userDao().getByEmail(email.trim())
                                        }

                                        if (existsEmail != null) {
                                            errorMessage = "El correo ya está en uso"
                                            return@launch
                                        }


                                        val newUser = withContext(Dispatchers.IO) {
                                            val user = User(
                                                username = username.trim(),
                                                email = email.trim(),
                                                password = password
                                            )
                                            db.userDao().insertUser(user)
                                            db.userDao().getByUsername(username.trim())!!
                                        }


                                        onRegistered(newUser)

                                    } catch (e: Exception) {
                                        errorMessage = "Error de registro: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = lightOrange)
                        ) {
                            Text("Enviar")
                        }



                        Spacer(Modifier.height(8.dp))

                        // Enlace a Login
                        TextButton(onClick = onLoginClick) {
                            Text(
                                buildAnnotatedString {
                                    append("¿Tienes cuenta? ")
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("Inicia sesión")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Previews

@Preview(
    name = "Register Phone",
    showBackground = true,
    device = Devices.PIXEL_4
)
@Preview(
    name = "Register Tablet Landscape",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
fun RegisterScreenPreview() {
    val context = LocalContext.current
    val db = remember {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    MeteoSpainTheme {
        RegisterScreen(
            db = db,
            onRegistered = {},
            onLoginClick = {}
        )
    }
}