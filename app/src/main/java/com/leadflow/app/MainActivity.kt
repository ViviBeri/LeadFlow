package com.leadflow.app

import android.os.Bundle
import android.widget.Toast
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
import com.leadflow.app.ui.ReportsScreen
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
    val leads by vm.leads.collectAsState()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                totalLeads = count,
                newLeads = leads.count { it.status == "New" },
                contactedLeads = leads.count { it.status == "Contacted" },
                interestedLeads = leads.count { it.status == "Interested" },
                convertedLeads = leads.count { it.status == "Converted" },
                notInterestedLeads = leads.count { it.status == "Not Interested" },
                onAdd = { nav.navigate("add") },
                onView = { nav.navigate("leads") },
                onExport = { nav.navigate("export") },
                onReports = { nav.navigate("reports") },
                onSettings = { nav.navigate("settings") },
                onDiscover = { nav.navigate("discover") }
            )
        }
        composable("discover") {
            DiscoveryScreen(
                onBack = { nav.popBackStack() },
                onSave = { place ->
                    val notes = "Source: OpenStreetMap\nCategory: ${place.category}\nMap: ${place.mapUrl}"
                    vm.addDiscoveredLeadIfNew(place.name, place.address, place.phone, notes, place.website) { added ->
                        Toast.makeText(
                            nav.context,
                            if (added) "Lead saved successfully" else "This lead is already saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
        composable("add") { AddLeadScreen(vm) { nav.popBackStack() } }
        composable("leads") { LeadsScreen(vm) { nav.popBackStack() } }
        composable("export") { ExportScreen(vm) { nav.popBackStack() } }
        composable("reports") { ReportsScreen(vm) { nav.popBackStack() } }
        composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
    }
}
