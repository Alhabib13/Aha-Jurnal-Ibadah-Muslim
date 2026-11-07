package com.islami.Aha.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.util.GenderProfile
import com.islami.Aha.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyDataUiState(
    val ikutPenelitian: Boolean = false,
    val isLoggedIn: Boolean = false,
    /** Akun anonim: bisa mundur penuh (hapus semua data + akun) dari layar ini. */
    val isAnonim: Boolean = false,
    /** ID pengguna, untuk permintaan penghapusan lewat web. */
    val idPengguna: String? = null,
    val showHapusAkunConfirm: Boolean = false,
    val isMenghapusAkun: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class PrivacyDataViewModel @Inject constructor(
    private val researchLogRepository: ResearchLogRepository,
    firebaseAuth: FirebaseAuth,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val authRepository: com.islami.Aha.data.repository.AuthRepository,
    private val habitDao: com.islami.Aha.data.local.HabitDao,
    private val userHabitDao: com.islami.Aha.data.local.UserHabitDao,
    private val habitCompletionDao: com.islami.Aha.data.local.HabitCompletionDao,
    private val dzikirHabitRepository: com.islami.Aha.data.dzikir.DzikirHabitRepository,
    private val tilawahState: com.islami.Aha.data.research.TilawahStateRepository,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PrivacyDataUiState(
            ikutPenelitian = researchLogRepository.researchConsent() == true,
            isLoggedIn = firebaseAuth.currentUser != null,
            isAnonim = firebaseAuth.currentUser?.isAnonymous == true,
            idPengguna = firebaseAuth.currentUser?.uid
        )
    )

    fun showHapusAkunConfirm(show: Boolean) = _uiState.update { it.copy(showHapusAkunConfirm = show) }

    /** Akun anonim: hapus semua data di server dan akun Firebase-nya, lalu data di HP. */
    fun hapusSemuaDataDanAkun() {
        _uiState.update { it.copy(showHapusAkunConfirm = false, isMenghapusAkun = true) }
        viewModelScope.launch {
            val hasil = authRepository.hapusAkunAnonim()
            if (hasil.isFailure) {
                _uiState.update {
                    it.copy(isMenghapusAkun = false, message = "Gagal menghapus. Periksa koneksi internet lalu coba lagi. Belum ada yang dihapus.")
                }
                return@launch
            }
            hapusDataLokal()
            _uiState.update {
                it.copy(
                    isMenghapusAkun = false,
                    isAnonim = false,
                    isLoggedIn = false,
                    idPengguna = null,
                    ikutPenelitian = false,
                    message = if (hasil.getOrNull() == true) "Semua datamu dan akunmu sudah dihapus."
                    else "Semua datamu sudah dihapus. Penghapusan akun diproses peneliti paling lambat 30 hari."
                )
            }
        }
    }

    /** Data di HP: habit, catatan, log penelitian, dzikir, tilawah, profil, dan pengingat. */
    private suspend fun hapusDataLokal() {
        runCatching {
            userHabitDao.getActiveReminderHabits().forEach {
                com.islami.Aha.util.NotificationScheduler.cancelHabitReminder(context, it.id)
            }
            com.islami.Aha.util.NotificationScheduler.cancelAllAlarms(context)
        }
        runCatching {
            habitDao.deleteAllHabits()
            userHabitDao.deleteAll()
            habitCompletionDao.deleteAll()
        }
        runCatching { researchLogRepository.clearLocal() }
        runCatching { dzikirHabitRepository.clearLocal() }
        runCatching { tilawahState.clearLocal() }
        sharedPreferences.edit().remove("hasSeeded").apply()
        UserPreferencesManager.clearAll()
        // Mundur dari penelitian: tercatat "tidak ikut" supaya layar persetujuan tidak muncul lagi.
        researchLogRepository.setResearchConsent(false, null)
    }
    val uiState: StateFlow<PrivacyDataUiState> = _uiState.asStateFlow()

    fun setIkutPenelitian(ikut: Boolean) {
        researchLogRepository.setResearchConsent(ikut)
        _uiState.update {
            it.copy(
                ikutPenelitian = ikut,
                message = if (ikut) "Terima kasih, catatanmu akan ikut membantu penelitian." else "Catatan baru tidak lagi dikirim untuk penelitian."
            )
        }
    }

    fun showDeleteConfirm(show: Boolean) = _uiState.update { it.copy(showDeleteConfirm = show) }

    fun deleteResearchData() {
        _uiState.update { it.copy(showDeleteConfirm = false, isDeleting = true) }
        viewModelScope.launch {
            val status = researchLogRepository.deleteMyResearchData()
            _uiState.update {
                it.copy(
                    isDeleting = false,
                    ikutPenelitian = false,
                    message = if (status.hasIssue) "Gagal menghapus. Periksa koneksi lalu coba lagi."
                    else "Data penelitianmu sudah dihapus. Catatan ibadah di HP tetap ada."
                )
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}

/** Pengaturan › Privasi & Data: ikut/berhenti penelitian, hapus data penelitian, profil terkunci. */
@Composable
fun PrivacyDataScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrivacyDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gender by UserPreferencesManager.gender.collectAsStateWithLifecycle()
    val usia by UserPreferencesManager.ageRange.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Text("Privasi & Data", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Kelompok("Penelitian") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setIkutPenelitian(!uiState.ikutPenelitian) }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Ikut penelitian", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (uiState.ikutPenelitian) "Catatan ibadahmu dikirim untuk penelitian skripsi."
                            else "Catatan tetap tersimpan di HP dan tidak dikirim untuk penelitian.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = uiState.ikutPenelitian, onCheckedChange = { viewModel.setIkutPenelitian(it) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isDeleting) { viewModel.showDeleteConfirm(true) }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Hapus data penelitianku", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Menghapus catatan yang sudah terkirim untuk penelitian. Akun dan catatan ibadahmu tetap ada.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.isDeleting) CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                    else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
            }
            Kelompok("Profil") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Profil ibadah", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        val jk = when (gender) {
                            GenderProfile.MALE -> "Laki-laki"
                            GenderProfile.FEMALE -> "Perempuan"
                            GenderProfile.UNSPECIFIED -> "Belum diatur"
                        }
                        Text(listOfNotNull(jk, usia?.label).joinToString(" · "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (gender != GenderProfile.UNSPECIFIED) {
                        Icon(Icons.Rounded.Lock, contentDescription = "Terkunci", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Kelompok("Lainnya") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(context, LegalDocumentActivity::class.java)
                                    .putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, LegalDocumentActivity.DOC_PRIVACY)
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text("Kebijakan privasi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (uiState.isAnonim) {
                    // Tanpa akun (anonim): mundur penuh dari sini, plus ID untuk permintaan lewat web.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isMenghapusAkun) { viewModel.showHapusAkunConfirm(true) }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Hapus semua data dan akun", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error)
                            Text(
                                "Menghapus semua catatan, profil, pengingat, dan akun tanpa-login di HP ini, di server maupun di HP. Tidak bisa dibatalkan.",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.isMenghapusAkun) CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                        else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                    uiState.idPengguna?.let { id ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("ID pengguna Aha", id))
                                    Toast.makeText(context, "ID pengguna disalin", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("ID pengguna", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(id, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Ketuk untuk menyalin. Dipakai bila meminta penghapusan lewat web.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text(
                        "Untuk menghapus akun beserta seluruh datanya, buka Pengaturan › Akun & Keamanan › Hapus akun permanen.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }

    if (uiState.showHapusAkunConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showHapusAkunConfirm(false) },
            title = { Text("Hapus semua data dan akun?") },
            text = {
                Text(
                    "Semua catatan ibadah, profil, habit, pengingat, dan data penelitianmu akan dihapus dari server dan dari HP ini, " +
                        "beserta akun tanpa-login-mu. Kamu juga berhenti ikut penelitian. Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::hapusSemuaDataDanAkun) {
                    Text("Hapus semua", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.showHapusAkunConfirm(false) }) { Text("Batal") } }
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteConfirm(false) },
            title = { Text("Hapus data penelitian?") },
            text = {
                Text(
                    if (uiState.isLoggedIn) "Semua catatanmu yang sudah dikirim untuk penelitian akan dihapus dari server, dan kamu berhenti ikut penelitian. Catatan ibadah di HP tetap ada."
                    else "Kamu sedang dalam mode tamu, jadi belum ada data yang dikirim. Kamu akan berhenti ikut penelitian."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::deleteResearchData) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { viewModel.showDeleteConfirm(false) }) { Text("Batal") } }
        )
    }
}

@Composable
private fun Kelompok(judul: String, isi: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            judul.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
        ) { isi() }
    }
}
