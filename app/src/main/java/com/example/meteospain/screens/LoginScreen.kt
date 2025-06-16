// LoginScreen.kt
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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.meteospain.InstagramFontFamily

@Composable
fun LoginScreen(
    db: AppDatabase,
    onLogin: (User) -> Unit,
    onRegisterClick: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Encabezado
                        Text(
                            text = "MeteoSpain",
                            fontSize = 40.sp,
                            fontFamily = InstagramFontFamily,
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(top = 22.dp)
                        )

                        Spacer(Modifier.height(32.dp))

                        // Título
                        Text(
                            text = "Inicia sesión",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Usuario") },
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

                        Spacer(Modifier.height(16.dp))

                        // Mensaje de error
                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Botón de inicio
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val user = withContext(Dispatchers.IO) {
                                        db.userDao().authenticate(username.trim(), password)
                                    }
                                    if (user != null) {
                                        onLogin(user)
                                    } else {
                                        errorMsg = "Usuario o contraseña incorrectos"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = lightOrange)
                        ) {
                            Text("Iniciar")
                        }

                        Spacer(Modifier.height(8.dp))

                        // Enlace a registro
                        TextButton(onClick = onRegisterClick) {
                            Text(
                                buildAnnotatedString {
                                    append("¿No tienes cuenta? ")
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("Regístrate")
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

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun LoginScreenPreview() {
    val context = LocalContext.current
    val db = remember {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    LoginScreen(
        db = db,
        onLogin = {},
        onRegisterClick = {}
    )
}