package com.example.meteospain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.meteospain.data.AppDatabase
import com.example.meteospain.data.User
import com.example.meteospain.ui.theme.MeteoSpainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.meteospain.data.Consulta
import com.example.meteospain.data.ConsultaDao
import com.example.meteospain.screens.ConsultaScreen
import com.example.meteospain.screens.LoginScreen
import com.example.meteospain.screens.RegisterScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Carga de la fuente en res/font/insta_font.ttf
val InstagramFontFamily = FontFamily(
    Font(R.font.insta_font, weight = FontWeight.Normal)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "user-database"
        )
            .fallbackToDestructiveMigration()
            .build()

        setContent {
            MeteoSpainTheme {
                val navController = rememberNavController()
                val currentUser = remember { mutableStateOf<User?>(null) }
                NavHost(navController = navController, startDestination = "login") {
                    composable("register") {
                        RegisterScreen(
                            db = db,
                            onRegistered = { user ->
                                // Al registrarse, guardamos el user y vamos a home
                                currentUser.value = user
                                navController.navigate("home")
                            },
                            onLoginClick = { navController.navigate("login") }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            db = db,
                            onLogin = { user ->
                                currentUser.value = user
                                navController.navigate("home")
                            },
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }
                    composable("home") {
                        currentUser.value?.let { user ->
                            HomeScreen(
                                navController = navController,
                                userName = user.username,
                                userEmail = user.email,
                                db = db,
                                userId = user.id
                            )
                        }  ?: run {
                            LaunchedEffect(Unit) {
                                navController.navigate("login")
                            }
                            Box {}
                        }
                    }
                    composable("consultas") {
                        currentUser.value?.let { user ->
                            ConsultaScreen(currentUserId = user.id)
                        } ?: run {
                            LaunchedEffect(Unit) {
                                navController.navigate("login")
                            }
                            Box {}
                        }
                    }
                    composable(
                        "consultas/{province}/{cmun}",
                        arguments = listOf(
                            navArgument("province") { type = NavType.StringType },
                            navArgument("cmun") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val provinceName = backStackEntry.arguments?.getString("province") ?: ""
                        val cmun = backStackEntry.arguments?.getInt("cmun") ?: 0
                        currentUser.value?.let { user ->
                            ConsultaScreen(
                                currentUserId = user.id,
                                initialProvince = provinceName,
                                initialCMUN = cmun
                            )
                        } ?: run {
                            //
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    userName: String,
    userEmail: String,
    db: AppDatabase,
    userId: Int
) {
    val lightOrange = Color(0xFFFFE0B2)
    val orange = Color(0xFFFFB74D)
    val white = Color.White
    val headerShape = RoundedCornerShape(8.dp)
    val consultaDao = remember { db.consultaDao() }

    val allConsultas by consultaDao.getAllConsultasByUser(userId)
        .collectAsState(initial = emptyList())

    val favoriteConsultas by remember(allConsultas) {
        derivedStateOf { allConsultas.filter { it.favorite } }
    }

    val recentConsultas by remember(allConsultas) {
        derivedStateOf { allConsultas.take(5) }
    }

    BoxWithConstraints {
        // responsive
        val horizontalPadding = if (maxWidth < 600.dp) 16.dp else 48.dp
        val isTablet = maxWidth >= 600.dp
        val buttonFraction = if (isTablet) 0.8f else 1f
        val buttonHeight = if (isTablet) 72.dp else 64.dp

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(white)
                        .statusBarsPadding()
                        .padding(horizontal = horizontalPadding, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bienvenido, $userName",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "¿Qué deseas hacer hoy?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = horizontalPadding, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { navController.navigate("login") }) {
                        Text("Deslogearse")
                    }
                }
            },
            containerColor = white
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentHeight()
                        .fillMaxWidth(buttonFraction)
                        .padding(horizontal = horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Button(
                            onClick = { navController.navigate("consultas") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            shape = headerShape,
                            colors = ButtonDefaults.buttonColors(containerColor = orange)
                        ) {
                            Text(
                                text = "Hacer una consulta",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    item {
                        var favExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.animateContentSize()) {
                            Surface(
                                color = lightOrange,
                                tonalElevation = 2.dp,
                                shape = headerShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { favExpanded = !favExpanded }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Consultas favoritas",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (favExpanded)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.Black
                                    )
                                }
                            }
                            if (favExpanded) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    favoriteConsultas.forEach { consulta ->
                                        ConsultaListItem(
                                            consulta = consulta,
                                            consultaDao = consultaDao,
                                            onClick = {
                                                navController.navigate("consultas/${consulta.province}/${consulta.result}")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        var histExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.animateContentSize()) {
                            Surface(
                                color = lightOrange,
                                tonalElevation = 2.dp,
                                shape = headerShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { histExpanded = !histExpanded }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Historial de consultas",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (histExpanded)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.Black
                                    )
                                }
                            }
                            if (histExpanded) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    if (recentConsultas.isEmpty()) {
                                        Text(
                                            text = "No hay consultas recientes",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        recentConsultas.forEach { consulta ->
                                            ConsultaListItem(
                                                consulta = consulta,
                                                consultaDao = consultaDao,
                                                onClick = {
                                                    navController.navigate("consultas/${consulta.province}/${consulta.result}")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsultaListItem(
    consulta: Consulta,
    consultaDao: ConsultaDao,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var currentConsulta by remember { mutableStateOf(consulta) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(consulta.id) {
        consultaDao.getAllConsultasByUser(consulta.userId)
            .map { list -> list.find { it.id == consulta.id } ?: consulta }
            .distinctUntilChanged()
            .collect { updatedConsulta ->
                currentConsulta = updatedConsulta
            }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = consulta.getFormattedLocation(),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    currentConsulta.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val updated = currentConsulta.copy(favorite = !currentConsulta.favorite)
                        consultaDao.update(updated)
                    }
                }
            ) {
                Icon(
                    imageVector = if (currentConsulta.favorite) Icons.Filled.Star
                    else Icons.Outlined.Star,
                    contentDescription = "Favorito",
                    tint = if (currentConsulta.favorite)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(
    name = "Phone Preview",
    showBackground = true,
    device = Devices.PIXEL_4
)
@Preview(
    name = "Tablet Landscape Preview",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
fun HomeScreenPreview() {
    MeteoSpainTheme {
        HomeScreen(
            navController = rememberNavController(),
            userName = "Ana García",
            userEmail = "ana.garcia@example.com",
            db = Room.inMemoryDatabaseBuilder(
                LocalContext.current,
                AppDatabase::class.java
            ).build(),
            userId = 1
        )
    }
}
