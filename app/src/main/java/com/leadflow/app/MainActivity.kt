package com.leadflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.leadflow.app.ui.AddLeadScreen
import com.leadflow.app.ui.DiscoveryScreen
import com.leadflow.app.ui.ExportScreen
import com.leadflow.app.ui.HomeScreen
import com.leadflow.app.ui.LeadFlowTheme
import com.leadflow.app.ui.LeadsScreen
import com.leadflow.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeadFlowTheme { LeadFlowApp() } }
    }
}

@Composable
fun LeadFlowApp() {
    val nav = rememberNavController()
    val vm: LeadViewModel = viewModel()
    val count by vm.totalCount.collectAsState()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                totalLeads = count,
                onAdd = { nav.navigate("add") },
                onView = { nav.navigate("leads") },
                onExport = { nav.navigate("export") },
                onSettings = { nav.navigate("settings") },
                onDiscover = { nav.navigate("discover") }
            )
        }
        composable("discover") { DiscoveryScreen { nav.popBackStack() } }
        composable("add") { AddLeadScreen(vm) { nav.popBackStack() } }
        composable("leads") { LeadsScreen(vm) { nav.popBackStack() } }
        composable("export") { ExportScreen(vm) { nav.popBackStack() } }
        composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
    }
}
