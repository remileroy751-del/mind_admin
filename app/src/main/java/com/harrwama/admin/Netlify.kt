package com.harrwama.admin

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/** Erreur lisible par l'utilisateur (message en français). */
class ApiException(message: String) : Exception(message)

class HttpResult(val code: Int, val body: ByteArray) {
    val text: String get() = String(body, Charsets.UTF_8)
}

object Http {
    fun call(
        method: String,
        url: String,
        token: String? = null,
        body: ByteArray? = null,
        contentType: String? = null
    ): HttpResult {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "HarrwamaAdmin/1.0")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.doOutput = true
                if (contentType != null) conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            return HttpResult(code, bytes)
        } finally {
            conn.disconnect()
        }
    }
}

class SiteInfo(val id: String, val publishedDeployId: String?)

class FileChange(val path: String, val bytes: ByteArray)

class NetlifyClient(private val token: String, siteUrl: String) {

    val host: String = normalizeHost(siteUrl)
    private val api = "https://api.netlify.com/api/v1"

    companion object {
        fun normalizeHost(input: String): String {
            var s = input.trim()
            s = s.removePrefix("https://").removePrefix("http://")
            s = s.substringBefore("/").substringBefore("?").substringBefore("#")
            return s.lowercase()
        }

        private val siteIdCache = HashMap<String, String>()
        private val formIdCache = HashMap<String, String>()

        fun sha1(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
            val sb = StringBuilder()
            for (b in digest) sb.append(String.format("%02x", b.toInt() and 0xff))
            return sb.toString()
        }

        private fun withSlash(p: String): String = if (p.startsWith("/")) p else "/$p"

        private fun encodePath(p: String): String {
            return p.removePrefix("/").split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
        }
    }

    /** Retrouve le site Netlify à partir de son adresse et vérifie la clé secrète. */
    fun connect(): SiteInfo {
        if (host.isEmpty()) throw ApiException("Adresse du site manquante.")
        if (token.isBlank()) throw ApiException("Clé secrète manquante.")

        var r = Http.call("GET", "$api/sites/$host", token)
        if (r.code == 401) throw ApiException("Clé secrète refusée par Netlify (401). Vérifiez le jeton saisi.")

        if (r.code == 404) {
            // Repli : on cherche le site dans la liste des sites du compte
            val list = Http.call("GET", "$api/sites?filter=all&per_page=100", token)
            if (list.code == 401) throw ApiException("Clé secrète refusée par Netlify (401). Vérifiez le jeton saisi.")
            if (list.code in 200..299) {
                val arr = JSONArray(list.text)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val candidates = listOf(
                        o.optString("url"), o.optString("ssl_url"),
                        o.optString("custom_domain"), o.optString("default_domain")
                    ).map { normalizeHost(it) }
                    if (candidates.contains(host)) {
                        r = Http.call("GET", "$api/sites/${o.getString("id")}", token)
                        break
                    }
                }
            }
        }
        if (r.code !in 200..299) {
            throw ApiException("Site introuvable sur ce compte Netlify (code ${r.code}). Vérifiez l'adresse et que la clé appartient au bon compte.")
        }
        val o = JSONObject(r.text)
        val published = o.optJSONObject("published_deploy")
        val pid = if (published != null) published.optString("id") else ""
        return SiteInfo(o.getString("id"), if (pid.isEmpty()) null else pid)
    }

    // ------------------------------------------------------------------ commandes

    private fun siteId(): String {
        val cached = siteIdCache[host]
        if (cached != null) return cached
        val id = connect().id
        siteIdCache[host] = id
        return id
    }

    private fun formIdFor(formName: String): String? {
        val cacheKey = host + "|" + formName
        val cached = formIdCache[cacheKey]
        if (cached != null) return cached
        val fr = Http.call("GET", "$api/sites/${siteId()}/forms", token)
        if (fr.code == 404) {
            siteIdCache.remove(host)
            throw ApiException("Site introuvable (404).")
        }
        if (fr.code !in 200..299) throw ApiException("Lecture des formulaires impossible (code ${fr.code}).")
        val forms = JSONArray(fr.text)
        for (i in 0 until forms.length()) {
            val f = forms.getJSONObject(i)
            if (f.optString("name") == formName) {
                val id = f.getString("id")
                formIdCache[cacheKey] = id
                return id
            }
        }
        return null // aucune commande reçue pour l'instant
    }

    /** Liste les commandes reçues via le formulaire du site (les plus récentes d'abord). */
    fun listOrders(): List<OrderItem> {
        val formId = formIdFor(ORDER_FORM) ?: return emptyList()
        val out = ArrayList<OrderItem>()
        var page = 1
        while (page <= 5) {
            val r = Http.call("GET", "$api/forms/$formId/submissions?per_page=100&page=$page", token)
            if (r.code == 401) throw ApiException("Clé secrète refusée par Netlify (401).")
            if (r.code !in 200..299) throw ApiException("Lecture des commandes impossible (code ${r.code}).")
            val arr = JSONArray(r.text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val d = o.optJSONObject("data") ?: continue
                val prenom = d.optString("prenom")
                val whatsapp = d.optString("whatsapp_complet")
                if (prenom.isEmpty() && whatsapp.isEmpty()) continue
                out.add(
                    OrderItem(
                        id = o.getString("id"),
                        createdAt = o.optString("created_at"),
                        produit = d.optString("produit"),
                        produitImage = d.optString("produit_image"),
                        produitPrix = d.optString("produit_prix"),
                        quantite = d.optString("quantite"),
                        prenom = prenom,
                        pays = d.optString("pays"),
                        whatsapp = whatsapp,
                        adresse = d.optString("adresse"),
                        dateLivraison = d.optString("date_livraison")
                    )
                )
            }
            if (arr.length() < 100) break
            page++
        }
        return out
    }

    /** Liste les messages reçus via « Nous écrire » (les plus récents d'abord). */
    fun listMessages(): List<MessageItem> {
        val formId = formIdFor(MESSAGE_FORM) ?: return emptyList()
        val out = ArrayList<MessageItem>()
        var page = 1
        while (page <= 5) {
            val r = Http.call("GET", "$api/forms/$formId/submissions?per_page=100&page=$page", token)
            if (r.code == 401) throw ApiException("Clé secrète refusée par Netlify (401).")
            if (r.code !in 200..299) throw ApiException("Lecture des messages impossible (code ${r.code}).")
            val arr = JSONArray(r.text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val d = o.optJSONObject("data") ?: continue
                val nom = d.optString("nom_complet")
                val msg = d.optString("message")
                if (nom.isEmpty() && msg.isEmpty()) continue
                out.add(
                    MessageItem(
                        id = o.getString("id"),
                        createdAt = o.optString("created_at"),
                        nom = nom,
                        pays = d.optString("pays"),
                        whatsapp = d.optString("whatsapp_complet"),
                        message = msg
                    )
                )
            }
            if (arr.length() < 100) break
            page++
        }
        return out
    }

    fun deleteOrder(id: String) {
        val r = Http.call("DELETE", "$api/submissions/$id", token)
        if (r.code !in 200..299 && r.code != 404) {
            throw ApiException("Suppression impossible (code ${r.code}).")
        }
    }

    /** Télécharge un fichier texte du site en ligne (null si 404). */
    fun fetchPublic(path: String): String? {
        val r = Http.call("GET", "https://$host/$path?t=${System.currentTimeMillis()}")
        if (r.code == 404) return null
        if (r.code !in 200..299) throw ApiException("Lecture de $path impossible (code ${r.code}).")
        return r.text
    }

    fun fetchPublicBytes(path: String): ByteArray? {
        val r = Http.call("GET", "https://$host/$path")
        return if (r.code in 200..299) r.body else null
    }

    /**
     * Publie une nouvelle version du site : on reprend TOUS les fichiers actuels (par empreinte SHA1),
     * on applique les modifications / suppressions, puis on envoie uniquement les fichiers nouveaux.
     */
    fun publish(changes: List<FileChange>, deletions: Set<String>, onStep: (String) -> Unit) {
        onStep("Connexion à Netlify…")
        val info = connect()
        val currentId = info.publishedDeployId
            ?: throw ApiException("Aucun déploiement publié trouvé. Publiez d'abord le site (zip) sur Netlify.")

        onStep("Lecture des fichiers actuels du site…")
        val fr = Http.call("GET", "$api/deploys/$currentId/files", token)
        if (fr.code !in 200..299) throw ApiException("Impossible de lire les fichiers du site (code ${fr.code}).")
        val files = LinkedHashMap<String, String>()
        val arr = JSONArray(fr.text)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            var p = o.optString("path")
            if (p.isEmpty()) p = o.optString("id")
            val sha = o.optString("sha")
            if (p.isNotEmpty() && sha.isNotEmpty()) files[withSlash(p)] = sha
        }
        // Garde-fou : on ne remplace jamais un site dont on n'a pas pu lire la liste complète
        if (!files.containsKey("/index.html")) {
            throw ApiException("Liste des fichiers du site incomplète : publication annulée par sécurité.")
        }

        for (d in deletions) files.remove(withSlash(d))
        val shaByPath = HashMap<String, String>()
        for (c in changes) {
            val sha = sha1(c.bytes)
            files[withSlash(c.path)] = sha
            shaByPath[withSlash(c.path)] = sha
        }

        onStep("Préparation de la publication…")
        val digest = JSONObject()
        for ((p, sha) in files) digest.put(p, sha)
        val payload = JSONObject().put("files", digest)
        val cr = Http.call(
            "POST", "$api/sites/${info.id}/deploys", token,
            payload.toString().toByteArray(Charsets.UTF_8), "application/json"
        )
        if (cr.code !in 200..299) throw ApiException("Création du déploiement refusée (code ${cr.code}) : ${shortBody(cr)}")
        val dep = JSONObject(cr.text)
        val newId = dep.getString("id")
        var required = stringList(dep.optJSONArray("required"))
        var state = dep.optString("state")

        // On attend que le déploiement soit prêt à recevoir les fichiers
        var tries = 0
        while ((state == "new" || state == "preparing" || state == "") && tries < 30) {
            Thread.sleep(1000)
            val g = Http.call("GET", "$api/deploys/$newId", token)
            if (g.code in 200..299) {
                val o = JSONObject(g.text)
                state = o.optString("state")
                val req = stringList(o.optJSONArray("required"))
                if (req.isNotEmpty()) required = req
            }
            tries++
        }

        // Envoi des fichiers réellement nouveaux
        val sent = HashSet<String>()
        val toSend = changes.filter {
            val s = shaByPath[withSlash(it.path)]
            s != null && required.contains(s)
        }
        var n = 0
        for (c in toSend) {
            val sha = shaByPath[withSlash(c.path)] ?: continue
            if (!sent.add(sha)) continue
            n++
            onStep("Envoi des fichiers ($n/${toSend.size})…")
            uploadFile(newId, c)
        }

        // Attente de la mise en ligne
        onStep("Mise en ligne en cours…")
        var waited = 0
        while (waited < 120) {
            val g = Http.call("GET", "$api/deploys/$newId", token)
            if (g.code in 200..299) {
                val o = JSONObject(g.text)
                val st = o.optString("state")
                if (st == "ready") return
                if (st == "error") {
                    throw ApiException("Netlify a signalé une erreur : " + o.optString("error_message", "inconnue"))
                }
            }
            Thread.sleep(2000)
            waited += 2
        }
        throw ApiException("La mise en ligne prend plus de temps que prévu. Vérifiez dans Netlify (onglet Deploys).")
    }

    private fun uploadFile(deployId: String, c: FileChange) {
        val url = "$api/deploys/$deployId/files/${encodePath(c.path)}"
        var attempt = 0
        while (true) {
            attempt++
            val r = Http.call("PUT", url, token, c.bytes, "application/octet-stream")
            if (r.code in 200..299) return
            if (attempt >= 5) throw ApiException("Envoi de ${c.path} refusé (code ${r.code}) : ${shortBody(r)}")
            Thread.sleep(1500)
        }
    }

    private fun stringList(a: JSONArray?): List<String> {
        val out = ArrayList<String>()
        if (a != null) for (i in 0 until a.length()) out.add(a.getString(i))
        return out
    }

    private fun shortBody(r: HttpResult): String = r.text.take(160)
}
