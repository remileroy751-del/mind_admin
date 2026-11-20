package com.harrwama.admin

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class AdminViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("harrwama_admin", Context.MODE_PRIVATE)

    var siteUrl by mutableStateOf(prefs.getString("site_url", "") ?: "")
        private set
    var token by mutableStateOf(prefs.getString("token", "") ?: "")
        private set

    // --- Contenu affiché / édité ---
    var home by mutableStateOf(HomeContent("", ""))
        private set
    private var homeSaved by mutableStateOf(HomeContent("", ""))
    val services = mutableStateListOf<ServiceItem>()
    val products = mutableStateListOf<ProductItem>()
    private var servicesChanges by mutableIntStateOf(0)
    private var productsChanges by mutableIntStateOf(0)

    // --- Commandes ---
    val orders = mutableStateListOf<OrderItem>()
    private val statusMap = mutableStateMapOf<String, Int>().also { m ->
        try {
            val o = JSONObject(prefs.getString("order_status", "{}") ?: "{}")
            for (k in o.keys()) m[k] = o.optInt(k, ST_NEW)
        } catch (e: Exception) {
            // préférences illisibles : on repart de zéro
        }
    }
    var ordersBusy by mutableStateOf(false)
        private set
    var ordersError by mutableStateOf<String?>(null)
        private set

    // --- État de l'écran ---
    var loaded by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var status by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var showSettings by mutableStateOf(false)

    // Images : à envoyer à la prochaine publication / gardées en mémoire pour l'affichage
    private val pendingFiles = LinkedHashMap<String, ByteArray>()
    private val localImages = ConcurrentHashMap<String, ByteArray>()
    private val deletedFiles = LinkedHashSet<String>()

    val homeChanged: Boolean get() = home != homeSaved
    val pendingCount: Int
        get() = (if (homeChanged) 1 else 0) + servicesChanges + productsChanges

    fun statusOf(id: String): Int = statusMap[id] ?: ST_NEW
    fun countOf(status: Int): Int = orders.count { statusOf(it.id) == status }

    init {
        if (siteUrl.isNotBlank() && token.isNotBlank()) refresh() else showSettings = true
    }

    // ------------------------------------------------------------------ connexion

    fun connect(url: String, key: String) {
        siteUrl = url.trim()
        token = key.trim()
        prefs.edit().putString("site_url", siteUrl).putString("token", token).apply()
        refresh()
    }

    fun refresh() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            status = "Connexion au site…"
            error = null
            try {
                val client = NetlifyClient(token, siteUrl)
                val loadedData = withContext(Dispatchers.IO) {
                    client.connect()
                    val h = client.fetchPublic("home.json")
                    val s = client.fetchPublic("services.json")
                    val p = client.fetchPublic("products.json")
                    if (h == null || s == null) {
                        throw ApiException(
                            "Les fichiers home.json / services.json sont absents du site. " +
                                "Publiez d'abord le NOUVEAU zip du site sur Netlify, puis réessayez."
                        )
                    }
                    Triple(parseHome(h), parseServices(s), parseProducts(p ?: "[]"))
                }
                home = loadedData.first
                homeSaved = loadedData.first
                services.clear(); services.addAll(loadedData.second)
                products.clear(); products.addAll(loadedData.third)
                servicesChanges = 0
                productsChanges = 0
                pendingFiles.clear()
                deletedFiles.clear()
                loaded = true
                showSettings = false
                status = "Chargement des commandes…"
                loadOrders(silent = false)
                OrderScheduler.schedule(getApplication<Application>())
            } catch (e: Exception) {
                loaded = false
                showSettings = true
                error = friendly(e)
            } finally {
                busy = false
                status = ""
            }
        }
    }

    // ------------------------------------------------------------------ commandes

    fun refreshOrders(silent: Boolean = false) {
        if (ordersBusy || busy || !loaded) return
        viewModelScope.launch { loadOrders(silent) }
    }

    private suspend fun loadOrders(silent: Boolean) {
        ordersBusy = true
        if (!silent) ordersError = null
        try {
            val list = withContext(Dispatchers.IO) { NetlifyClient(token, siteUrl).listOrders() }
            val sorted = list.sortedByDescending { it.createdAt }
            val seen = HashSet<String>(prefs.getStringSet("seen_orders", emptySet()) ?: emptySet())
            val initDone = prefs.getBoolean("seen_init", false)
            val fresh = if (initDone) sorted.filter { !seen.contains(it.id) } else emptyList()
            orders.clear()
            orders.addAll(sorted)
            for (o in sorted) seen.add(o.id)
            prefs.edit().putStringSet("seen_orders", seen).putBoolean("seen_init", true).apply()
            ordersError = null
            if (fresh.size == 1) {
                message = "🔔 Nouvelle commande de " + fresh[0].prenom
            } else if (fresh.size > 1) {
                message = "🔔 " + fresh.size + " nouvelles commandes"
            }
        } catch (e: Exception) {
            if (!silent) ordersError = friendly(e)
        } finally {
            ordersBusy = false
        }
    }

    fun setOrderStatus(id: String, st: Int) {
        statusMap[id] = st
        val o = JSONObject()
        for ((k, v) in statusMap) o.put(k, v)
        prefs.edit().putString("order_status", o.toString()).apply()
    }

    fun deleteOrder(id: String) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            status = "Suppression de la commande…"
            try {
                withContext(Dispatchers.IO) { NetlifyClient(token, siteUrl).deleteOrder(id) }
                val idx = orders.indexOfFirst { it.id == id }
                if (idx >= 0) orders.removeAt(idx)
                statusMap.remove(id)
                val o = JSONObject()
                for ((k, v) in statusMap) o.put(k, v)
                prefs.edit().putString("order_status", o.toString()).apply()
                message = "Commande supprimée."
            } catch (e: Exception) {
                message = "Échec de la suppression : " + friendly(e)
            } finally {
                busy = false
                status = ""
            }
        }
    }

    // ------------------------------------------------------------------ accueil

    fun updateHome(titre: String? = null, texte: String? = null) {
        home = HomeContent(titre ?: home.titre, texte ?: home.texte)
    }

    /** Valide la modification de l'accueil et met le site à jour immédiatement. */
    fun saveHome(titre: String, texte: String) {
        home = HomeContent(titre.trim(), texte.trim())
        publish()
    }

    // ------------------------------------------------------------------ services

    fun editService(index: Int, titre: String, domaine: String, description: String, image: ByteArray?) {
        if (index !in services.indices) return
        val old = services[index]
        var path = old.image
        if (image != null) {
            forgetImage(old.image)
            path = "assets/services/s-${System.currentTimeMillis()}.jpg"
            pendingFiles[path] = image
            localImages[path] = image
        }
        services[index] = ServiceItem(domaine.trim(), titre.trim(), description.trim(), path)
        servicesChanges++
        publish()
    }

    fun addService(titre: String, domaine: String, description: String, image: ByteArray?) {
        var path = ""
        if (image != null) {
            path = "assets/services/s-${System.currentTimeMillis()}.jpg"
            pendingFiles[path] = image
            localImages[path] = image
        }
        services.add(ServiceItem(domaine.trim(), titre.trim(), description.trim(), path))
        servicesChanges++
    }

    fun deleteService(index: Int) {
        if (index !in services.indices) return
        val item = services.removeAt(index)
        forgetImage(item.image)
        servicesChanges++
    }

    // ------------------------------------------------------------------ produits

    fun addProduct(nom: String, categorie: String, prix: String, description: String, image: ByteArray?) {
        if (products.size >= MAX_PRODUCTS) {
            message = "Maximum $MAX_PRODUCTS produits : supprimez-en un avant d'en ajouter."
            return
        }
        var path = ""
        if (image != null) {
            path = "assets/products/p-${System.currentTimeMillis()}.jpg"
            pendingFiles[path] = image
            localImages[path] = image
        }
        products.add(ProductItem(nom.trim(), categorie.trim(), prix.trim(), description.trim(), path))
        productsChanges++
    }

    fun editProduct(index: Int, nom: String, categorie: String, prix: String, description: String, image: ByteArray?) {
        if (index !in products.indices) return
        val old = products[index]
        var path = old.image
        if (image != null) {
            forgetImage(old.image)
            path = "assets/products/p-${System.currentTimeMillis()}.jpg"
            pendingFiles[path] = image
            localImages[path] = image
        }
        products[index] = ProductItem(nom.trim(), categorie.trim(), prix.trim(), description.trim(), path)
        productsChanges++
        publish()
    }

    fun deleteProduct(index: Int) {
        if (index !in products.indices) return
        val item = products.removeAt(index)
        forgetImage(item.image)
        productsChanges++
    }

    private fun forgetImage(path: String) {
        if (path.isBlank()) return
        if (pendingFiles.remove(path) == null) {
            // image déjà en ligne : on la retire du site à la prochaine publication
            if (path.startsWith("assets/services/") || path.startsWith("assets/products/")) {
                deletedFiles.add(path)
            }
        }
        localImages.remove(path)
    }

    // ------------------------------------------------------------------ images

    fun loadThumb(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        ImageCache.get(path)?.let { return it }
        var bytes: ByteArray? = localImages[path]
        if (bytes == null) {
            try {
                bytes = NetlifyClient(token, siteUrl).fetchPublicBytes(path)
            } catch (e: Exception) {
                bytes = null
            }
        }
        if (bytes == null) return null
        val bmp = ImageUtil.decodeSampled(bytes, 360) ?: return null
        val img = bmp.asImageBitmap()
        ImageCache.put(path, img)
        return img
    }

    // ------------------------------------------------------------------ publication

    fun publish() {
        if (busy || pendingCount == 0) return
        viewModelScope.launch {
            busy = true
            error = null
            status = "Préparation…"
            try {
                val changes = ArrayList<FileChange>()
                val doHome = homeChanged
                val doServices = servicesChanges > 0
                val doProducts = productsChanges > 0
                if (doHome) changes.add(FileChange("home.json", homeJson().toByteArray(Charsets.UTF_8)))
                if (doServices) changes.add(FileChange("services.json", servicesJson().toByteArray(Charsets.UTF_8)))
                if (doProducts) changes.add(FileChange("products.json", productsJson().toByteArray(Charsets.UTF_8)))
                for ((p, b) in pendingFiles) changes.add(FileChange(p, b))
                val deletions = deletedFiles.toSet()

                val client = NetlifyClient(token, siteUrl)
                withContext(Dispatchers.IO) {
                    client.publish(changes, deletions) { step -> status = step }
                }

                homeSaved = home
                servicesChanges = 0
                productsChanges = 0
                pendingFiles.clear()
                deletedFiles.clear()
                message = "Publié ✓ Les changements sont visibles sur le site dans quelques instants."
            } catch (e: Exception) {
                message = "Échec de la publication : " + friendly(e)
            } finally {
                busy = false
                status = ""
            }
        }
    }

    // ------------------------------------------------------------------ JSON

    private fun parseHome(t: String): HomeContent {
        val o = JSONObject(t)
        return HomeContent(o.optString("titre"), o.optString("texte"))
    }

    private fun parseServices(t: String): List<ServiceItem> {
        val a = JSONArray(t)
        val out = ArrayList<ServiceItem>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(ServiceItem(o.optString("domaine"), o.optString("titre"), o.optString("description"), o.optString("image")))
        }
        return out
    }

    private fun parseProducts(t: String): List<ProductItem> {
        val a = JSONArray(t)
        val out = ArrayList<ProductItem>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(
                ProductItem(
                    o.optString("nom"), o.optString("categorie"), o.optString("prix"),
                    o.optString("description"), o.optString("image")
                )
            )
        }
        return out
    }

    private fun homeJson(): String {
        val o = JSONObject()
        o.put("titre", home.titre)
        o.put("texte", home.texte)
        return o.toString(2)
    }

    private fun servicesJson(): String {
        val a = JSONArray()
        for (s in services) {
            val o = JSONObject()
            o.put("domaine", s.domaine)
            o.put("titre", s.titre)
            o.put("description", s.description)
            o.put("image", s.image)
            a.put(o)
        }
        return a.toString(2)
    }

    private fun productsJson(): String {
        val a = JSONArray()
        for (p in products) {
            val o = JSONObject()
            o.put("nom", p.nom)
            o.put("categorie", p.categorie)
            o.put("prix", p.prix)
            o.put("description", p.description)
            o.put("image", p.image)
            a.put(o)
        }
        return a.toString(2)
    }

    private fun friendly(e: Exception): String {
        if (e is ApiException) return e.message ?: "Erreur inconnue."
        if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException || e is java.net.ConnectException) {
            return "Pas de connexion Internet (ou Netlify injoignable). Réessayez."
        }
        return (e.message ?: e.javaClass.simpleName)
    }
}
