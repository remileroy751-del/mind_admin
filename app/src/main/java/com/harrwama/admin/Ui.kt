package com.harrwama.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
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
            "Connectez l'application à votre site Netlify pour modifier l'accueil, les produits et les services.",
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
    val titles = listOf("Accueil", "Produits", "Services")
    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    var confirmPublish by remember { mutableStateOf(false) }
    var confirmRefresh by remember { mutableStateOf(false) }

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
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Marine,
                    contentColor = Color.White,
                    indicator = {}
                ) {
                    titles.forEachIndexed { i, t ->
                        Tab(
                            selected = tab == i,
                            onClick = { tab = i },
                            modifier = Modifier.background(if (tab == i) Bordeaux else Marine),
                            selectedContentColor = Color.White,
                            unselectedContentColor = PaleBlue,
                            text = { Text(t, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (vm.pendingCount > 0) {
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
                else -> ServicesTab(vm)
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
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Page d'accueil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Marine)

        OutlinedTextField(
            value = vm.home.titre,
            onValueChange = { vm.updateHome(titre = it) },
            label = { Text("Grand titre") },
            supportingText = { Text("Entourez des mots avec des étoiles pour les mettre en couleur : *votre santé*") },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.home.texte,
            onValueChange = { vm.updateHome(texte = it) },
            label = { Text("Texte sous le grand titre") },
            minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Aperçu", fontWeight = FontWeight.SemiBold, color = Slate)
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
    }
}

// ============================================================ onglet Produits

@Composable
fun ProductsTab(vm: AdminViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableIntStateOf(-1) }

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
            ItemCard(vm, p.image, p.nom, sub, p.description) { deleteIndex = i }
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
            ItemCard(vm, s.image, s.titre, s.domaine, s.description) { deleteIndex = i }
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

// ============================================================ composants communs

@Composable
fun Thumb(vm: AdminViewModel, path: String) {
    val state by produceState<Pair<Boolean, ImageBitmap?>>(Pair(false, null), path) {
        value = Pair(true, withContext(Dispatchers.IO) { vm.loadThumb(path) })
    }
    val done = state.first
    val bmp = state.second
    Box(
        Modifier
            .size(76.dp)
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Bordeaux)
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
