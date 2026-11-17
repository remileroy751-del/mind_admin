# Harrwama Admin — application Android de mise à jour du site

Modifie le site Harrwama Mind (hébergé sur Netlify) depuis le téléphone :
- onglet **Accueil** : grand titre + texte sous le titre
- onglet **Produits** : ajouter / supprimer (image + nom + petite description, 10 max)
- onglet **Services** : ajouter / supprimer (image + titre + petite description)

## Compiler (automatique)
1. Créez un dépôt GitHub (privé conseillé) et envoyez-y **le contenu de ce dossier**
   (le dossier caché `.github` doit être à la racine du dépôt).
2. La compilation démarre toute seule (onglet **Actions**). Durée : ~5 à 8 minutes.
3. Téléchargez `HarrwamaAdmin.apk` :
   - onglet **Releases** du dépôt (release « apk-latest »), ou
   - dans l'exécution du workflow > **Artifacts**.
4. Installez l'APK sur le téléphone (autoriser « sources inconnues » si demandé).

Chaque nouvelle compilation utilise la même clé de signature (`app/debug.keystore`) :
l'appli se met à jour par-dessus l'ancienne, sans désinstallation.

## Utilisation
Au premier lancement, saisissez l'adresse du site (https://xxx.netlify.app) et la clé secrète
Netlify (User settings > Applications > Personal access tokens). Modifiez, puis touchez
**Publier** : un seul déploiement Netlify est créé pour toutes vos modifications.
