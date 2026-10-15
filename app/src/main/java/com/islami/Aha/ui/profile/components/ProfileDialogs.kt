package com.islami.Aha.ui.profile.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.profile.UserInfo

private val HijauTengah = Color(0xFF0D7C5F)
private val HijauAksen = Color(0xFF2ECC9B)
private val HijauPucat = Color(0xFFD4F3E8)
private val TeksDiHijau = Color(0xFF062B1F)
private val Emas = Color(0xFFF2C66D)
private const val NAMA_MAKS = 40

/** Lembar "Ubah profil": foto, nama tampilan (jeda 30 hari antar perubahan), email hanya dibaca. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    userInfo: UserInfo,
    pendingAvatarUri: Uri?,
    canChangeUsername: Boolean,
    daysUntilUsernameChange: Int,
    isSaving: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (newAvatarUri: Uri?, newUsername: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(userInfo.name) { mutableStateOf(userInfo.name) }
    var konfirmasiHapus by remember { mutableStateOf(false) }
    val hasAvatarChange = pendingAvatarUri != null
    val hasNameChange = nameInput.trim().isNotBlank() && nameInput.trim() != userInfo.name && canChangeUsername
    val canSave = (hasAvatarChange || hasNameChange) && !isSaving
    val punyaFoto = !userInfo.avatarUri.isNullOrBlank() || pendingAvatarUri != null
    val simpan = {
        if (canSave) onSave(if (hasAvatarChange) pendingAvatarUri else null, if (hasNameChange) nameInput.trim() else null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.profile_edit_profile_title),
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Foto: ketuk untuk mengganti.
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(HijauPucat)
                        .border(3.dp, if (hasAvatarChange) Emas else HijauAksen, CircleShape)
                        .clickable(role = Role.Button, enabled = !isSaving, onClick = onPickPhoto)
                ) {
                    FotoProfil(userInfo, pendingAvatarUri)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HijauAksen)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable(role = Role.Button, enabled = !isSaving, onClick = onPickPhoto)
                ) { Icon(Icons.Rounded.CameraAlt, contentDescription = stringResource(R.string.profile_pick_photo_action), tint = TeksDiHijau, modifier = Modifier.size(18.dp)) }
                if (hasAvatarChange) {
                    Text(
                        stringResource(R.string.profile_photo_new_badge),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TeksDiHijau,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Emas)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                TombolPil(Icons.Rounded.PhotoLibrary, if (punyaFoto) "Ganti foto" else "Pilih foto", HijauAksen, enabled = !isSaving, onClick = onPickPhoto)
                if (punyaFoto) {
                    TombolPil(Icons.Rounded.DeleteOutline, stringResource(R.string.profile_remove_photo_action), MaterialTheme.colorScheme.error, enabled = !isSaving, onClick = { konfirmasiHapus = true })
                }
            }

            Spacer(Modifier.height(22.dp))

            // Nama tampilan
            Text("Nama tampilan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        1.5.dp,
                        if (canChangeUsername && hasNameChange) HijauAksen else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    if (canChangeUsername) Icons.Rounded.Person else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(NAMA_MAKS) },
                    enabled = canChangeUsername && !isSaving,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = if (canChangeUsername) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    cursorBrush = SolidColor(HijauAksen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { simpan() }),
                    modifier = Modifier.weight(1f)
                )
                if (canChangeUsername) {
                    Text("${nameInput.length}/$NAMA_MAKS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (canChangeUsername) "Nama bisa diubah sekali setiap 30 hari."
                else pluralStringResource(R.plurals.profile_username_locked_days_format, daysUntilUsernameChange, daysUntilUsernameChange),
                fontSize = 12.sp,
                color = if (canChangeUsername) MaterialTheme.colorScheme.onSurfaceVariant else Emas
            )

            if (userInfo.email.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Email", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        userInfo.email, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text("Tidak bisa diubah", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = simpan,
                enabled = canSave,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = HijauAksen, contentColor = TeksDiHijau),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan perubahan", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }

    if (konfirmasiHapus) {
        DialogHapusFoto(
            adaFotoBaru = hasAvatarChange,
            onHapus = { konfirmasiHapus = false; onRemovePhoto() },
            onBatal = { konfirmasiHapus = false }
        )
    }
}

@Composable
private fun DialogHapusFoto(adaFotoBaru: Boolean, onHapus: () -> Unit, onBatal: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBatal,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Hapus foto profil?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (adaFotoBaru) "Foto yang baru dipilih juga dibatalkan. Profilmu akan menampilkan inisial nama."
                else "Profilmu akan menampilkan inisial nama. Kamu bisa memilih foto lagi kapan saja.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onHapus) { Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onBatal) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun FotoProfil(userInfo: UserInfo, pendingAvatarUri: Uri?) {
    val model: Any? = when {
        pendingAvatarUri != null -> pendingAvatarUri
        !userInfo.avatarUri.isNullOrBlank() -> remember(userInfo.avatarUri) {
            if (userInfo.avatarUri.startsWith("data:image")) {
                android.util.Base64.decode(userInfo.avatarUri.substringAfter("base64,"), android.util.Base64.DEFAULT)
            } else userInfo.avatarUri
        }
        else -> null
    }
    if (model != null) {
        coil.compose.AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.profile_photo_cd),
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(userInfo.avatarInitial, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = HijauTengah)
    }
}

@Composable
private fun TombolPil(ikon: ImageVector, label: String, warna: Color, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(warna.copy(alpha = 0.12f))
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(ikon, contentDescription = null, tint = warna, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = warna)
    }
}
