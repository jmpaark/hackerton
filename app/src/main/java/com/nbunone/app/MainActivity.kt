package com.nbunone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nbunone.app.data.AppRepository
import com.nbunone.app.ui.NbunoneTheme
import com.nbunone.app.ui.screens.CreateTeamScreen
import com.nbunone.app.ui.screens.HomeScreen
import com.nbunone.app.ui.screens.LoginScreen
import com.nbunone.app.ui.screens.ProfessorDashboardScreen
import com.nbunone.app.ui.screens.ProfessorTeamScreen
import com.nbunone.app.ui.screens.ReportScreen
import com.nbunone.app.ui.screens.SettingsScreen
import com.nbunone.app.ui.screens.TeamDetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(applicationContext)
        setContent {
            NbunoneTheme {
                AppNav()
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav: NavHostController = rememberNavController()
    val vm: AppViewModel = viewModel()
    val data by vm.data.collectAsState()

    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(vm = vm, data = data, onLoggedIn = { isProfessor ->
                nav.navigate(if (isProfessor) "prof" else "home") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(
                vm = vm, data = data,
                onCreateTeam = { nav.navigate("createTeam") },
                onOpenTeam = { nav.navigate("team/$it") },
                onLogout = {
                    vm.logout()
                    nav.navigate("login") { popUpTo(0) }
                }
            )
        }
        composable("createTeam") {
            CreateTeamScreen(vm = vm, onDone = { nav.popBackStack() })
        }
        composable("team/{teamId}") { entry ->
            val teamId = entry.arguments?.getString("teamId") ?: return@composable
            TeamDetailScreen(vm = vm, data = data, teamId = teamId, onBack = { nav.popBackStack() })
        }
        composable("prof") {
            ProfessorDashboardScreen(
                vm = vm, data = data,
                onOpenTeam = { nav.navigate("profTeam/$it") },
                onSettings = { nav.navigate("settings") },
                onLogout = {
                    vm.logout()
                    nav.navigate("login") { popUpTo(0) }
                }
            )
        }
        composable("profTeam/{teamId}") { entry ->
            val teamId = entry.arguments?.getString("teamId") ?: return@composable
            ProfessorTeamScreen(
                vm = vm, data = data, teamId = teamId,
                onBack = { nav.popBackStack() },
                onOpenReport = { nav.navigate("report/$teamId") }
            )
        }
        composable("report/{teamId}") { entry ->
            val teamId = entry.arguments?.getString("teamId") ?: return@composable
            ReportScreen(vm = vm, data = data, teamId = teamId, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(vm = vm, data = data, onBack = { nav.popBackStack() })
        }
    }
}
