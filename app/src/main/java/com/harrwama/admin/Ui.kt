package com.harrwama.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val Marine = Color(0xFF101E33)
val Bordeaux = Color(0xFF7C1E2C)
val BordeauxLight = Color(0xFFC1495A)
val Paper = Color(0xFFF4F6FA)
val Slate = Color(0xFF4A5A72)
val PaleBlue = Color(0xFFAFC0D6)

@Composable
fun HarrwamaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Bordeaux,
            onPrimary = Color.White,
            secondary = Marine,
            background = Paper,
            surface = Color.White
        ),
        content = content
    )
}

// ============================================================ racine

@Composable
fun AdminApp() {
    val vm: AdminViewModel = viewModel()
    when {
        vm.showSettings -> SettingsScreen(vm)
        !vm.loaded -> Box(Modifier.fillMaxSize().background(Marine))
        else -> MainScreen(vm)
    }
    if (vm.busy) BusyDialog(vm.status)
}

@Composable
fun BusyDialog(status: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Veuillez patienter") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(16.dp))
                Text(if (status.isBlank()) "Chargement…" else status)
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// ============================================================ réglages / connexion

@Composable
fun SettingsScreen(vm: AdminViewModel) {
    var url by remember { mutableStateOf(vm.siteUrl) }
    var key by remember { mutableStateOf(vm.token) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Harrwama Admin", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Marine)
        Text(
            "Connectez l'application à votre site Netlify pour modifier l'accueil, les produits et les services, et gérer les commandes.",
            color = Slate
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Adresse du site") },
            placeholder = { Text("https://mon-site.netlify.app") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Clé secrète Netlify") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { showKey = !showKey }) {
            Text(if (showKey) "Masquer la clé" else "Afficher la clé")
        }

        val err = vm.error
        if (err != null) {
            Text(err, color = Color(0xFFB3261E))
        }

        Button(
            onClick = { vm.connect(url, key) },
            enabled = url.isNotBlank() && key.isNotBlank() && !vm.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Enregistrer et se connecter")
        }
        if (vm.loaded) {
            OutlinedButton(
                onClick = { vm.error = null; vm.showSettings = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annuler")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Où trouver la clé secrète ?", fontWeight = FontWeight.Bold)
                Text(
                    "1. Sur app.netlify.com, ouvrez le menu de votre profil > User settings.\n" +
                        "2. Applications > Personal access tokens > New access token.\n" +
                        "3. Donnez un nom (ex : Harrwama Admin), validez, puis copiez la clé affichée " +
                        "(elle ne s'affiche qu'une seule fois) et collez-la ci-dessus.",
                    fontSize = 13.sp,
                    color = Slate
                )
            }
        }
    }
}

// ============================================================ écran principal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: AdminViewModel) {
    val newCount = vm.countOf(ST_NEW)
    val unread = vm.unreadCount()
    val titles = listOf(
        "Accueil", "Produits", "Services",
        if (newCount > 0) "Commandes ($newCount)" else "Commandes",
        if (unread > 0) "Messagerie ($unread)" else "Messagerie"
    )
    var tab by remember {
        mutableIntStateOf(if (AppNav.openMessages) 4 else if (AppNav.openOrders) 3 else 0)
    }
    val snackbar = remember { SnackbarHostState() }
    var confirmPublish by remember { mutableStateOf(false) }
    var confirmRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(AppNav.openOrders) {
        if (AppNav.openOrders) {
            tab = 3
            AppNav.openOrders = false
            vm.refreshOrders(silent = true)
        }
    }

    LaunchedEffect(AppNav.openMessages) {
        if (AppNav.openMessages) {
            tab = 4
            AppNav.openMessages = false
            vm.refreshOrders(silent = true)
        }
    }

    // Vérifie les nouvelles commandes et messages toutes les 30 secondes tant que l'appli est ouverte
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            vm.refreshOrders(silent = true)
        }
    }

    LaunchedEffect(vm.message) {
        val m = vm.message
        if (m != null) {
            vm.message = null
            snackbar.showSnackbar(m)
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Harrwama Mind", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Marine,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = {
                            if (vm.pendingCount > 0) confirmRefresh = true else vm.refresh()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recharger depuis le site")
                        }
                        IconButton(onClick = { vm.showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Réglages")
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = tab,
                    containerColor = Marine,
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    indicator = {},
                    divider = {}
                ) {
                    titles.forEachIndexed { i, t ->
                        Tab(
                            selected = tab == i,
                            onClick = { tab = i },
                            modifier = Modifier.background(if (tab == i) Bordeaux else Marine),
                            selectedContentColor = Color.White,
                            unselectedContentColor = PaleBlue,
                            text = { Text(t, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (vm.pendingCount > 0 && tab < 3) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Button(
                        onClick = { confirmPublish = true },
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp)
                    ) {
                        val n = vm.pendingCount
                        Text("Publier $n modification" + (if (n > 1) "s" else "") + " sur le site")
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeTab(vm)
                1 -> ProductsTab(vm)
                2 -> ServicesTab(vm)
                3 -> OrdersTab(vm)
                else -> MessagesTab(vm)
            }
        }
    }

    if (confirmPublish) {
        ConfirmDialog(
            title = "Publier sur le site ?",
            text = "Le site en ligne sera mis à jour avec vos modifications. " +
                "Chaque publication compte comme un déploiement Netlify (certains forfaits en limitent " +
                "le nombre par mois) : regroupez vos changements avant de publier.",
            confirmLabel = "Publier",
            onConfirm = { confirmPublish = false; vm.publish() },
            onDismiss = { confirmPublish = false }
        )
    }
    if (confirmRefresh) {
        ConfirmDialog(
            title = "Abandonner les modifications ?",
            text = "Recharger depuis le site effacera les modifications non publiées.",
            confirmLabel = "Recharger",
            onConfirm = { confirmRefresh = false; vm.refresh() },
            onDismiss = { confirmRefresh = false }
        )
    }
}

// ============================================================ onglet Accueil

fun styledTitle(s: String): AnnotatedString = buildAnnotatedString {
    val parts = s.split("*")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1 && i < parts.size - 1) {
            withStyle(SpanStyle(color = BordeauxLight)) { append(part) }
        } else {
            append(part)
        }
    }
}

@Composable
fun HomeTab(vm: AdminViewModel) {
    var editing by remember { mutableStateOf(false) }
    var editingPhone by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Page d'accueil",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Marine,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Bordeaux)
            }
        }
        Text("Texte actuellement affiché sur le site :", color = Slate, fontSize = 13.sp)

        Card(colors = CardDefaults.cardColors(containerColor = Marine), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    styledTitle(vm.home.titre),
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold
                )
                Text(vm.home.texte, color = PaleBlue, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }

        Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Modifier le titre et le texte")
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Téléphone principal du site")
                Text(vm.phone, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Marine)
                Text(
                    "Ce numéro sert au bouton WhatsApp, au lien « Écrire sur WhatsApp » du bas de page " +
                        "et aux boutons « Contacter pour ce service ». Il reste cliquable sur le site.",
                    fontSize = 12.sp, color = Slate
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { editingPhone = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Modifier le numéro de téléphone")
                }
            }
        }
    }

    if (editingPhone) {
        EditPhoneScreen(
            initPhone = vm.phone,
            onDismiss = { editingPhone = false },
            onSave = { t ->
                editingPhone = false
                vm.savePhone(t)
            }
        )
    }

    if (editing) {
        EditHomeScreen(
            initTitre = vm.home.titre,
            initTexte = vm.home.texte,
            onDismiss = { editing = false },
            onSave = { t, x ->
                editing = false
                vm.saveHome(t, x)
            }
        )
    }
}

@Composable
fun EditHomeScreen(
    initTitre: String,
    initTexte: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var titre by remember { mutableStateOf(initTitre) }
    var texte by remember { mutableStateOf(initTexte) }
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Marine).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("← Annuler", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Modifier l'accueil", color = PaleBlue, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = titre,
                        onValueChange = { titre = it },
                        label = { Text("Grand titre") },
                        supportingText = { Text("Entourez des mots avec des étoiles pour les mettre en couleur : *votre santé*") },
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Texte sous le grand titre") },
                        minLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Aperçu", fontWeight = FontWeight.SemiBold, color = Slate)
                    Card(colors = CardDefaults.cardColors(containerColor = Marine), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                styledTitle(titre),
                                color = Color.White, fontSize = 24.sp, lineHeight = 30.sp,
                                fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold
                            )
                            Text(texte, color = PaleBlue, fontSize = 14.sp, lineHeight = 21.sp)
                        }
                    }
                    val e = err
                    if (e != null) Text(e, color = Color(0xFFB3261E), fontSize = 13.sp)
                }
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Button(
                        onClick = {
                            if (titre.isBlank() || texte.isBlank()) {
                                err = "Le titre et le texte ne peuvent pas être vides."
                            } else if (titre.trim() == initTitre.trim() && texte.trim() == initTexte.trim()) {
                                onDismiss()
                            } else {
                                onSave(titre, texte)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp)
                    ) { Text("Valider et mettre à jour le site") }
                }
            }
        }
    }
}

// ============================================================ onglet Produits

@Composable
fun ProductsTab(vm: AdminViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var editIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "${vm.products.size} / $MAX_PRODUCTS produits",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Marine
            )
        }
        item {
            Button(
                onClick = { showAdd = true },
                enabled = vm.products.size < MAX_PRODUCTS,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter un produit")
            }
        }
        if (vm.products.isEmpty()) {
            item { Text("Aucun produit publié.", color = Slate) }
        }
        itemsIndexed(vm.products) { i, p ->
            val sub = listOf(p.categorie, p.prix).filter { it.isNotBlank() }.joinToString(" · ")
            ItemCard(vm, p.image, p.nom, sub, p.description, onEdit = { editIndex = i }, onDelete = { deleteIndex = i })
        }
    }

    if (showAdd) {
        ItemDialog(
            heading = "Nouveau produit",
            nameLabel = "Nom du produit",
            tagLabel = "Catégorie (facultatif)",
            priceLabel = "Prix (facultatif)",
            onDismiss = { showAdd = false },
            onSave = { name, tag, price, desc, img ->
                vm.addProduct(name, tag, price, desc, img)
                showAdd = false
            }
        )
    }
    if (editIndex >= 0 && editIndex < vm.products.size) {
        val cur = vm.products[editIndex]
        val idx = editIndex
        EditItemScreen(
            vm = vm,
            heading = "Modifier le produit",
            nameLabel = "Nom du produit",
            tagLabel = "Catégorie (facultatif)",
            priceLabel = "Prix (facultatif)",
            initName = cur.nom,
            initTag = cur.categorie,
            initPrice = cur.prix,
            initDesc = cur.description,
            currentImage = cur.image,
            onDismiss = { editIndex = -1 },
            onSave = { name, tag, price, desc, img ->
                editIndex = -1
                vm.editProduct(idx, name, tag, price, desc, img)
            }
        )
    }
    if (deleteIndex >= 0 && deleteIndex < vm.products.size) {
        ConfirmDialog(
            title = "Supprimer ce produit ?",
            text = "« ${vm.products[deleteIndex].nom} » sera retiré du site à la prochaine publication.",
            confirmLabel = "Supprimer",
            onConfirm = { vm.deleteProduct(deleteIndex); deleteIndex = -1 },
            onDismiss = { deleteIndex = -1 }
        )
    }
}

// ============================================================ onglet Services

@Composable
fun ServicesTab(vm: AdminViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var editIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "${vm.services.size} service" + (if (vm.services.size > 1) "s" else ""),
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Marine
            )
        }
        item {
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter un service")
            }
        }
        if (vm.services.isEmpty()) {
            item { Text("Aucun service publié.", color = Slate) }
        }
        itemsIndexed(vm.services) { i, s ->
            ItemCard(vm, s.image, s.titre, s.domaine, s.description, onEdit = { editIndex = i }, onDelete = { deleteIndex = i })
        }
    }

    if (showAdd) {
        ItemDialog(
            heading = "Nouveau service",
            nameLabel = "Titre du service",
            tagLabel = "Domaine (facultatif, ex : Finance)",
            priceLabel = null,
            onDismiss = { showAdd = false },
            onSave = { name, tag, _, desc, img ->
                vm.addService(name, tag, desc, img)
                showAdd = false
            }
        )
    }
    if (editIndex >= 0 && editIndex < vm.services.size) {
        val cur = vm.services[editIndex]
        val idx = editIndex
        EditItemScreen(
            vm = vm,
            heading = "Modifier le service",
            nameLabel = "Titre du service",
            tagLabel = "Domaine (facultatif, ex : Finance)",
            priceLabel = null,
            initName = cur.titre,
            initTag = cur.domaine,
            initPrice = "",
            initDesc = cur.description,
            currentImage = cur.image,
            onDismiss = { editIndex = -1 },
            onSave = { name, tag, _, desc, img ->
                editIndex = -1
                vm.editService(idx, name, tag, desc, img)
            }
        )
    }
    if (deleteIndex >= 0 && deleteIndex < vm.services.size) {
        ConfirmDialog(
            title = "Supprimer ce service ?",
            text = "« ${vm.services[deleteIndex].titre} » sera retiré du site à la prochaine publication.",
            confirmLabel = "Supprimer",
            onConfirm = { vm.deleteService(deleteIndex); deleteIndex = -1 },
            onDismiss = { deleteIndex = -1 }
        )
    }
}

fun isValidPhone(t: String): Boolean {
    val v = t.trim()
    if (!v.startsWith("+")) return false
    if (v.count { it == '+' } != 1) return false
    if (!v.all { it.isDigit() || it == ' ' || it == '+' }) return false
    val digits = v.count { it.isDigit() }
    return digits in 8..15
}

@Composable
fun EditPhoneScreen(initPhone: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var tel by remember { mutableStateOf(initPhone) }
    var err by remember { mutableStateOf<String?>(null) }
    val digits = tel.filter { it.isDigit() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Marine).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("← Annuler", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Téléphone du site", color = PaleBlue, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = tel,
                        onValueChange = { tel = it.filter { c -> c.isDigit() || c == ' ' || c == '+' }; err = null },
                        label = { Text("Numéro principal (WhatsApp)") },
                        supportingText = { Text("Format international : + puis l'indicatif et le numéro. Ex : +226 70 70 70 21") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SectionLabel("Aperçu sur le site")
                            Text(if (tel.isBlank()) "—" else tel.trim(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Lien WhatsApp : wa.me/" + digits, fontSize = 12.sp, color = Slate)
                        }
                    }
                    val e = err
                    if (e != null) Text(e, color = Color(0xFFB3261E), fontSize = 13.sp)
                }
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Button(
                        onClick = {
                            if (!isValidPhone(tel)) {
                                err = "Numéro invalide : commencez par + et l'indicatif du pays (8 à 15 chiffres)."
                            } else if (tel.trim() == initPhone.trim()) {
                                onDismiss()
                            } else {
                                onSave(tel)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp)
                    ) { Text("Valider et mettre à jour le site") }
                }
            }
        }
    }
}

// ============================================================ onglet Messagerie

@Composable
fun MessagesTab(vm: AdminViewModel) {
    var openId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Messagerie",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Marine,
                    modifier = Modifier.weight(1f)
                )
                if (vm.ordersBusy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedButton(onClick = { vm.refreshOrders(silent = false) }, enabled = !vm.ordersBusy) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Actualiser")
                }
            }
        }
        item {
            Text(
                "Messages envoyés par les visiteurs via le bouton « Nous écrire » du site. " +
                    "Touchez un nom pour lire le message.",
                color = Slate, fontSize = 13.sp
            )
        }
        val err = vm.messagesError
        if (err != null) {
            item { Text(err, color = Color(0xFFB3261E), fontSize = 13.sp) }
        }
        if (vm.messages.isEmpty() && err == null) {
            item { Text("Aucun message pour le moment. Vous serez averti dès qu'un visiteur vous écrit.", color = Slate) }
        }
        items(vm.messages, key = { it.id }) { m ->
            MessageRow(m, unread = !vm.isRead(m.id)) {
                vm.markRead(m.id)
                openId = m.id
            }
        }
    }

    val open = openId?.let { id -> vm.messages.firstOrNull { it.id == id } }
    if (open != null) {
        MessageDetail(vm, open) { openId = null }
    }
}

@Composable
fun MessageRow(m: MessageItem, unread: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (unread) Bordeaux else Color.Transparent)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    m.nom,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(formatCreated(m.createdAt), color = Slate, fontSize = 12.sp)
            }
            Text("›", fontSize = 28.sp, color = Slate)
        }
    }
}

@Composable
fun MessageDetail(vm: AdminViewModel, m: MessageItem, onClose: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Marine).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) { Text("← Retour", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Message reçu", color = PaleBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("Message")
                            Text(m.message, fontSize = 17.sp, lineHeight = 24.sp)
                            Text("— " + m.nom, fontWeight = FontWeight.Bold, color = Bordeaux)
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel("Expéditeur")
                            Text(m.nom, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            if (m.pays.isNotBlank()) Text(m.pays, color = Slate)
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val msg = "Bonjour " + m.nom + ", ici HARRWAMA MIND. " +
                                        "Merci pour votre message : « " + m.message + " »."
                                    if (!openWhatsapp(context, m.whatsapp, msg)) {
                                        Toast.makeText(context, "WhatsApp n'a pas pu s'ouvrir sur ce téléphone.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Text("💬  " + m.whatsapp, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Touchez le numéro pour ouvrir WhatsApp et répondre directement à ce visiteur.",
                                fontSize = 12.sp, color = Slate
                            )
                        }
                    }

                    Text("Reçu le " + formatCreated(m.createdAt), fontSize = 12.sp, color = Slate)
                }

                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { vm.markUnread(m.id); onClose() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Marquer comme non lu") }
                        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Supprimer ce message", color = Color(0xFFB3261E))
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Supprimer ce message ?",
            text = "Le message de " + m.nom + " sera définitivement supprimé de Netlify.",
            confirmLabel = "Supprimer",
            onConfirm = { confirmDelete = false; onClose(); vm.deleteMessage(m.id) },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ============================================================ onglet Commandes

fun openWhatsapp(context: Context, phone: String, message: String): Boolean {
    val digits = phone.filter { it.isDigit() }
    if (digits.isEmpty()) return false
    return try {
        val uri = Uri.parse("https://wa.me/" + digits + "?text=" + Uri.encode(message))
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, color = Bordeaux, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@Composable
fun OrdersTab(vm: AdminViewModel) {
    val context = LocalContext.current
    var sub by remember { mutableIntStateOf(0) }
    var openId by remember { mutableStateOf<String?>(null) }
    val labels = listOf("Nouvelles", "En cours", "Livrées")
    val list = vm.orders.filter { vm.statusOf(it.id) == sub }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = sub,
            containerColor = Color.White,
            contentColor = Bordeaux
        ) {
            labels.forEachIndexed { i, l ->
                Tab(
                    selected = sub == i,
                    onClick = { sub = i },
                    selectedContentColor = Bordeaux,
                    unselectedContentColor = Slate,
                    text = {
                        Text(
                            l + " (" + vm.countOf(i) + ")",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                        )
                    }
                )
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Gestion des commandes",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Marine,
                        modifier = Modifier.weight(1f)
                    )
                    if (vm.ordersBusy) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedButton(onClick = { vm.refreshOrders(silent = false) }, enabled = !vm.ordersBusy) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Actualiser")
                    }
                }
            }
            val err = vm.ordersError
            if (err != null) {
                item { Text(err, color = Color(0xFFB3261E), fontSize = 13.sp) }
            }
            if (list.isEmpty() && err == null) {
                item {
                    Text(
                        when (sub) {
                            0 -> "Aucune nouvelle commande pour le moment. Vous serez averti dès qu'un client commande sur le site."
                            1 -> "Aucune commande en cours de traitement."
                            else -> "Aucune commande livrée."
                        },
                        color = Slate
                    )
                }
            }
            items(list, key = { it.id }) { o ->
                OrderRow(o) { openId = o.id }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔔 Alertes de nouvelles commandes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "L'appli vérifie les commandes toutes les 15 minutes environ, même fermée. " +
                                "Pour ne rien rater : autorisez les notifications et retirez l'appli de " +
                                "l'optimisation de batterie de votre téléphone.",
                            fontSize = 12.sp, color = Slate
                        )
                        TextButton(onClick = {
                            try {
                                context.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Réglages indisponibles sur ce téléphone.", Toast.LENGTH_LONG).show()
                            }
                        }) { Text("Ouvrir les réglages batterie") }
                    }
                }
            }
        }
    }

    val open = openId?.let { id -> vm.orders.firstOrNull { it.id == id } }
    if (open != null) {
        OrderDetail(vm, open) { openId = null }
    }
}

@Composable
fun OrderRow(o: OrderItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(o.prenom, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    o.quantite + " × " + o.produit,
                    color = Bordeaux, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Livraison souhaitée le " + formatDelivery(o.dateLivraison),
                    color = Slate, fontSize = 12.sp
                )
            }
            Text("›", fontSize = 28.sp, color = Slate)
        }
    }
}

@Composable
fun OrderDetail(vm: AdminViewModel, o: OrderItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val st = vm.statusOf(o.id)
    var confirmDelete by remember { mutableStateOf(false) }
    val stLabel = when (st) {
        ST_NEW -> "Nouvelle commande"
        ST_PROCESSING -> "En cours de traitement"
        else -> "Livrée"
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Marine).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) { Text("← Retour", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stLabel, color = PaleBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Thumb(vm, o.produitImage, 96.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                SectionLabel("Produit commandé")
                                Text(o.produit, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                if (o.produitPrix.isNotBlank()) Text(o.produitPrix, color = Slate, fontSize = 13.sp)
                                Text("Quantité : " + o.quantite + " boîte(s)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel("Client")
                            Text(o.prenom, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            if (o.pays.isNotBlank()) Text(o.pays, color = Slate)
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val msg = "Bonjour " + o.prenom + ", ici HARRWAMA MIND. " +
                                        "Nous avons bien reçu votre commande : " + o.quantite + " boîte(s) de " + o.produit + ". " +
                                        "Vous souhaitez être livré(e) le " + formatDelivery(o.dateLivraison) + " à : " + o.adresse + ". " +
                                        "Pouvez-vous confirmer, afin que nous fixions ensemble l'heure du rendez-vous de livraison ? Merci !"
                                    if (openWhatsapp(context, o.whatsapp, msg)) {
                                        if (st == ST_NEW) vm.setOrderStatus(o.id, ST_PROCESSING)
                                    } else {
                                        Toast.makeText(context, "WhatsApp n'a pas pu s'ouvrir sur ce téléphone.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Text("💬  " + o.whatsapp, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Touchez le numéro pour ouvrir WhatsApp : la discussion avec le client s'ouvre avec un message " +
                                    "prêt à envoyer pour confirmer et fixer le rendez-vous de livraison.",
                                fontSize = 12.sp, color = Slate
                            )
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel("Livraison")
                            Text(
                                "Date souhaitée : " + formatDelivery(o.dateLivraison),
                                fontWeight = FontWeight.Bold, fontSize = 16.sp
                            )
                            Text(o.adresse)
                            Text("Paiement à la livraison", color = Bordeaux, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text("Commande reçue le " + formatCreated(o.createdAt), fontSize = 12.sp, color = Slate)
                }

                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (st) {
                            ST_NEW -> {
                                Button(
                                    onClick = { vm.setOrderStatus(o.id, ST_PROCESSING) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) { Text("Prendre en charge (passer en cours)") }
                            }
                            ST_PROCESSING -> {
                                Button(
                                    onClick = { vm.setOrderStatus(o.id, ST_DELIVERED); onClose() },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) { Text("✓ Marquer comme livrée") }
                                OutlinedButton(
                                    onClick = { vm.setOrderStatus(o.id, ST_NEW) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Remettre dans « Nouvelles »") }
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = { vm.setOrderStatus(o.id, ST_PROCESSING) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Remettre « En cours »") }
                                TextButton(
                                    onClick = { confirmDelete = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Supprimer cette commande", color = Color(0xFFB3261E)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Supprimer cette commande ?",
            text = "La commande de " + o.prenom + " sera définitivement supprimée de Netlify.",
            confirmLabel = "Supprimer",
            onConfirm = { confirmDelete = false; onClose(); vm.deleteOrder(o.id) },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ============================================================ composants communs

@Composable
fun Thumb(vm: AdminViewModel, path: String, size: Dp = 76.dp) {
    val state by produceState<Pair<Boolean, ImageBitmap?>>(Pair(false, null), path) {
        value = Pair(true, withContext(Dispatchers.IO) { vm.loadThumb(path) })
    }
    val done = state.first
    val bmp = state.second
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE6EAF0)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (path.isNotBlank() && !done) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("Pas d'image", fontSize = 10.sp, color = Slate)
        }
    }
}

@Composable
fun ItemCard(
    vm: AdminViewModel,
    image: String,
    title: String,
    subtitle: String,
    description: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Thumb(vm, image)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Bordeaux, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Slate,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Marine)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Bordeaux)
                }
            }
        }
    }
}

@Composable
fun EditItemScreen(
    vm: AdminViewModel,
    heading: String,
    nameLabel: String,
    tagLabel: String,
    priceLabel: String?,
    initName: String,
    initTag: String,
    initPrice: String,
    initDesc: String,
    currentImage: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, ByteArray?) -> Unit
) {
    var name by remember { mutableStateOf(initName) }
    var tag by remember { mutableStateOf(initTag) }
    var price by remember { mutableStateOf(initPrice) }
    var desc by remember { mutableStateOf(initDesc) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loadingImage by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loadingImage = true
            err = null
            scope.launch {
                try {
                    imageBytes = withContext(Dispatchers.IO) { ImageUtil.prepareJpeg(context, uri) }
                } catch (e: Exception) {
                    err = "Image illisible : " + (e.message ?: "")
                }
                loadingImage = false
            }
        }
    }
    val preview: ImageBitmap? = remember(imageBytes) {
        val b = imageBytes
        if (b == null) null else ImageUtil.decodeSampled(b, 600)?.asImageBitmap()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Marine).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("← Annuler", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        heading, color = PaleBlue, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 12.dp)
                    )
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Photo", fontWeight = FontWeight.SemiBold, color = Slate)
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Thumb(vm, currentImage, 150.dp)
                    }
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !loadingImage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                loadingImage -> "Traitement de l'image…"
                                imageBytes != null -> "Choisir une autre photo"
                                currentImage.isNotBlank() -> "Changer la photo"
                                else -> "Ajouter une photo"
                            }
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(nameLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { tag = it },
                        label = { Text(tagLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (priceLabel != null) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text(priceLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Petite description") },
                        minLines = 5,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val e = err
                    if (e != null) Text(e, color = Color(0xFFB3261E), fontSize = 13.sp)
                }

                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Button(
                        enabled = !loadingImage,
                        onClick = {
                            if (name.isBlank()) {
                                err = "Le titre / nom est obligatoire."
                            } else if (imageBytes == null &&
                                name.trim() == initName.trim() && tag.trim() == initTag.trim() &&
                                price.trim() == initPrice.trim() && desc.trim() == initDesc.trim()
                            ) {
                                onDismiss() // rien n'a changé
                            } else {
                                onSave(name, tag, price, desc, imageBytes)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp)
                    ) { Text("Valider et mettre à jour le site") }
                }
            }
        }
    }
}

@Composable
fun ItemDialog(
    heading: String,
    nameLabel: String,
    tagLabel: String,
    priceLabel: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, ByteArray?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loadingImage by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loadingImage = true
            err = null
            scope.launch {
                try {
                    imageBytes = withContext(Dispatchers.IO) { ImageUtil.prepareJpeg(context, uri) }
                } catch (e: Exception) {
                    err = "Image illisible : " + (e.message ?: "")
                }
                loadingImage = false
            }
        }
    }
    val preview: ImageBitmap? = remember(imageBytes) {
        val b = imageBytes
        if (b == null) null else ImageUtil.decodeSampled(b, 400)?.asImageBitmap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(tagLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                if (priceLabel != null) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(priceLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Petite description") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !loadingImage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            loadingImage -> "Traitement de l'image…"
                            imageBytes != null -> "Changer l'image"
                            else -> "Choisir une image"
                        }
                    )
                }
                val e = err
                if (e != null) Text(e, color = Color(0xFFB3261E), fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loadingImage,
                onClick = {
                    if (name.isBlank()) {
                        err = "Le titre / nom est obligatoire."
                    } else {
                        onSave(name, tag, price, desc, imageBytes)
                    }
                }
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
