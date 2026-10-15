package com.islami.Aha.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.islami.Aha.R
import com.islami.Aha.ui.auth.createGoogleSignInClient
import com.islami.Aha.ui.settings.components.DeleteAccountConfirmationDialog
import com.islami.Aha.ui.settings.components.ReAuthDialog

private val MerahBahaya = Color(0xFFFF8A80)

/**
 * Pengaturan › Akun & Keamanan: email & verifikasi, kata sandi (hanya akun email),
 * keluar, dan hapus akun. Akun Google tidak punya kata sandi Aha; login ulang sebelum
 * hapus akun dilakukan dengan memilih akun Google lagi.
 */
@Composable
fun AccountSecurityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (String?) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var halamanUbahSandi by rememberSaveable { mutableStateOf(false) }
    val googleClient = remember(context) { createGoogleSignInClient(context) }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val token = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)?.idToken
        }.getOrNull()
        viewModel.confirmGoogleReAuthDelete(token) { onNavigateToLogin(null) }
    }

    LaunchedEffect(Unit) {
        if (uiState.isLoggedIn && !uiState.isGoogleUser) viewModel.refreshEmailVerificationStatus()
    }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(uiState.requestGoogleReAuth) {
        if (uiState.requestGoogleReAuth) {
            viewModel.googleReAuthLaunched()
            val client = googleClient
            if (client == null) {
                Toast.makeText(context, context.getString(R.string.settings_snackbar_delete_account_failed), Toast.LENGTH_SHORT).show()
            } else {
                // Keluar dulu dari sesi Google di HP supaya pemilih akun selalu muncul.
                client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
            }
        }
    }

    BackHandler(enabled = halamanUbahSandi) { halamanUbahSandi = false }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = { if (halamanUbahSandi) halamanUbahSandi = false else onNavigateBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                if (halamanUbahSandi) "Ubah kata sandi" else "Akun & Keamanan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (halamanUbahSandi) {
            UbahSandiForm(
                email = uiState.userEmail,
                isSubmitting = uiState.isChangingPassword,
                onSubmit = { lama, baru, ulang -> viewModel.submitPasswordChange(lama, baru, ulang) },
                onLupa = viewModel::sendForgotPasswordFromSettings
            )
            LaunchedEffect(uiState.passwordChanged) {
                if (uiState.passwordChanged) {
                    viewModel.passwordChangedHandled()
                    halamanUbahSandi = false
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Kelompok("Email") {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(uiState.userEmail.ifBlank { "-" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val terverifikasi = uiState.isGoogleUser || uiState.isEmailVerified
                            Lencana(
                                teks = if (terverifikasi) "Terverifikasi" else "Belum terverifikasi",
                                warna = if (terverifikasi) MaterialTheme.colorScheme.primary else Color(0xFFE2B65A),
                                ok = terverifikasi
                            )
                            if (uiState.isGoogleUser) {
                                Lencana("Masuk dengan Google", MaterialTheme.colorScheme.onSurfaceVariant, ok = null)
                            }
                        }
                        if (!uiState.isGoogleUser && !uiState.isEmailVerified) {
                            val cooldown = uiState.verificationResendCooldownSeconds
                            OutlinedButton(
                                onClick = viewModel::sendEmailVerificationFromSecurity,
                                enabled = cooldown == 0 && !uiState.isSecurityActionInProgress(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (cooldown > 0) "Kirim ulang verifikasi (${cooldown} dtk)" else "Kirim ulang verifikasi")
                            }
                            TextButton(onClick = viewModel::refreshEmailVerificationStatus, enabled = !uiState.isSecurityActionInProgress()) {
                                Text("Sudah verifikasi? Muat ulang status")
                            }
                        }
                    }
                }
                Kelompok("Kata sandi") {
                    if (uiState.isGoogleUser) {
                        Text(
                            "Kamu masuk dengan akun Google, jadi tidak ada kata sandi di Aha. Kata sandi diatur di akun Google-mu.",
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                        )
                    } else {
                        BarisMenu("Ubah kata sandi", "Perlu kata sandi lama") { halamanUbahSandi = true }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        BarisMenu(
                            "Lupa kata sandi?",
                            "Kirim link reset ke email",
                            sibuk = uiState.isSendingResetPasswordEmail
                        ) { viewModel.sendForgotPasswordFromSecurity() }
                    }
                }
                // Keluar akun ada di halaman utama Pengaturan; zona ini hanya untuk hapus akun.
                Kelompok("Zona berbahaya") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isDeletingAccount) { viewModel.showDeleteAccountConfirmation() }
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text("Hapus akun permanen", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MerahBahaya)
                        Text(
                            if (uiState.isGoogleUser) "Kamu akan diminta memilih akun Google lagi untuk konfirmasi."
                            else "Semua catatan ibadah dan data penelitian ikut terhapus. Tidak bisa dibatalkan.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (uiState.isDeletingAccount) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Menghapus akun…", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (uiState.showDeleteAccountConfirmation) {
        DeleteAccountConfirmationDialog(
            isDeleting = uiState.isDeletingAccount,
            onConfirm = { viewModel.confirmDeleteAccount { onNavigateToLogin(null) } },
            onDismiss = viewModel::hideDeleteAccountConfirmation
        )
    }
    if (uiState.showReAuthDialog) {
        ReAuthDialog(
            isDeleting = uiState.isDeletingAccount,
            onConfirm = { password -> viewModel.confirmReAuthDelete(password) { onNavigateToLogin(null) } },
            onDismiss = viewModel::hideReAuthDialog
        )
    }
}

@Composable
private fun UbahSandiForm(
    email: String,
    isSubmitting: Boolean,
    onSubmit: (String, String, String) -> Unit,
    onLupa: () -> Unit
) {
    var lama by rememberSaveable { mutableStateOf("") }
    var baru by rememberSaveable { mutableStateOf("") }
    var ulang by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Kami akan mengirim email pemberitahuan ke $email setelah kata sandi diganti.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            KolomSandi("Kata sandi lama", lama) { lama = it }
            KolomSandi("Kata sandi baru", baru, petunjuk = "Minimal 6 karakter, berbeda dari kata sandi lama.") { baru = it }
            KolomSandi("Ulangi kata sandi baru", ulang) { ulang = it }
            TextButton(onClick = onLupa) { Text("Lupa kata sandi lama?", fontWeight = FontWeight.Bold) }
        }
        Button(
            onClick = { onSubmit(lama, baru, ulang) },
            enabled = !isSubmitting && lama.isNotBlank() && baru.isNotBlank() && ulang.isNotBlank(),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .height(56.dp)
        ) {
            if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Simpan kata sandi", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KolomSandi(label: String, nilai: String, petunjuk: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = petunjuk?.let { { Text(it) } },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Lencana(teks: String, warna: Color, ok: Boolean?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(warna.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (ok != null) {
            Icon(
                if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = warna,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(teks, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = warna)
    }
}

@Composable
private fun BarisMenu(judul: String, sub: String, sibuk: Boolean = false, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !sibuk, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(judul, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sibuk) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
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
        Spacer(Modifier.height(0.dp))
    }
}
