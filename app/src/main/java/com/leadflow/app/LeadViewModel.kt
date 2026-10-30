package com.leadflow.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leadflow.app.data.AppDatabase
import com.leadflow.app.data.Lead
import com.leadflow.app.data.Prefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeadViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).leadDao()
    val prefs = Prefs(app)

    private val query = MutableStateFlow("")
    val searchText: StateFlow<String> = query

    @OptIn(ExperimentalCoroutinesApi::class)
    val leads: StateFlow<List<Lead>> = query
        .flatMapLatest { dao.search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = dao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setQuery(q: String) { query.value = q }

    fun addLead(name: String, address: String, phone: String, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            dao.insert(Lead(businessName = name, address = address, phone = phone, notes = notes))
            onDone()
        }
    }

    fun addDiscoveredLeadIfNew(
        name: String,
        address: String,
        phone: String,
        notes: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val added = dao.insertIfAbsent(
                Lead(businessName = name, address = address, phone = phone, notes = notes)
            )
            onResult(added)
        }
    }

    fun updateLeadStatus(lead: Lead, status: String) {
        viewModelScope.launch { dao.updateStatus(lead.id, status) }
    }

    fun updateLeadNotes(lead: Lead, notes: String) {
        viewModelScope.launch { dao.updateNotes(lead.id, notes) }
    }

    fun updateLeadTags(lead: Lead, tags: String) {
        viewModelScope.launch { dao.updateTags(lead.id, tags) }
    }

    fun updateLeadPriority(lead: Lead, priority: String) {
        viewModelScope.launch { dao.updatePriority(lead.id, priority) }
    }

    fun updateLeadFollowUpDate(lead: Lead, date: String) {
        viewModelScope.launch { dao.updateFollowUpDate(lead.id, date) }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch { dao.delete(lead) }
    }

    suspend fun allLeads(): List<Lead> = dao.getAll()
}
