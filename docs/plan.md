# Plan d'implémentation V1

Ce plan découpe la V1 décrite dans [spec.md](spec.md) en étapes livrables. Chaque étape se termine par un build vert (`./gradlew assembleDebug lint testDebugUnitTest`) et un commit sur `main`.

**Objectif intermédiaire** : à la fin de l'étape 4, l'app réveille le vrai PC du salon.

## Architecture

- **Un seul module Gradle** `:app` tant que l'app reste petite. Les frontières passent par les packages.
- **Packages** sous `fr.sybers.debout` :

| Package | Contenu | Dépendances Android |
|---|---|---|
| `domain` | `MacAddress`, `MagicPacket`, `Broadcast`, la machine à états du réveil, le modèle `Machine` | Aucune (Kotlin pur, testable en JVM) |
| `network` | Envoi UDP, sonde TCP, résolution de hostname, état de la connexion du téléphone | Oui |
| `data` | Room (machines), DataStore (préférences), repository | Oui |
| `ui` | Écrans Compose, ViewModels, thème | Oui |
| `tile` | `TileService` Quick Settings, raccourcis d'app | Oui |
| `tracking` | Suivi d'un réveil (WorkManager) et sa notification | Oui |

- **Injection de dépendances manuelle**, via un conteneur créé dans une classe `Application`. Pas de Hilt : le graphe est petit.
- **Temps injecté** (horloge et délais passés en paramètre) dans la machine à états, pour la tester en temps virtuel avec `kotlinx-coroutines-test`.
- **Versions** : à chaque nouvelle dépendance, prendre la dernière version stable publiée depuis au moins deux semaines (voir « Environnement »).

## Étapes

### 1. Cœur réseau, en TDD — `domain`

- `MacAddress.parse(String)` :
  - accepte `AA:BB:CC:DD:EE:FF`, `aa-bb-cc-dd-ee-ff`, `aabbccddeeff`, ainsi que les espaces autour ;
  - rejette les longueurs fausses et les caractères non hexadécimaux, avec un résultat typé plutôt qu'une exception.
- `MagicPacket.of(mac)` : 102 octets, 6 × `FF` suivis de 16 répétitions de la MAC.
- `Broadcast.of(ip, prefixLength)` : adresse de broadcast d'un sous-réseau IPv4 (par exemple `192.168.1.42/24` → `192.168.1.255`).
- Tests unitaires JVM pour chacun.

**Terminé quand** tous les cas de parsing et de construction sont couverts par des tests verts.

### 2. Machine à états du réveil — `domain`

- Les 6 états de la spec : `Unknown`, `Offline`, `Sending`, `Booting`, `Online`, `NoResponse`.
- Entrées : demande de réveil, résultat de sonde (répond / ne répond pas), perte du réseau local, écoulement du temps.
- Règles :
  - une demande de réveil sur `Online` ne déclenche aucun envoi et produit un événement « Déjà allumé » ;
  - aucun réveil n'est possible en `Unknown`, `Sending` ni `Booting` ;
  - `Booting` passe à `NoResponse` après ~90 s sans réponse ;
  - un réveil est possible depuis `NoResponse` (« réessayer ») ;
  - l'envoi part en 3 paquets espacés de ~100 ms.
- La machine à états ne fait aucune I/O : l'envoi et la sonde sont des interfaces injectées.
- Tests en temps virtuel pour toutes les transitions et les timeouts.

**Terminé quand** chaque transition du tableau de la spec a son test.

### 3. Persistance et saisie manuelle — `data` + `network` + `ui`

- `network` :
  - envoi UDP en broadcast (port 9 par défaut) ;
  - détection de la connexion Wi-Fi/Ethernet du téléphone via `ConnectivityManager` ;
  - lecture du sous-réseau du téléphone, sans permission de localisation.
- Entité Room `Machine`, avec les champs de la spec : nom, type, MAC, IP, hostname, broadcast, port WoL, port de sonde, ordre.
- Repository exposant un `Flow<List<Machine>>`.
- Écran de saisie manuelle :
  - champ MAC tolérant avec validation en direct ;
  - champs avancés repliés (broadcast, port WoL) ;
  - aide « Où trouver ma MAC ? » par OS.
- Édition, suppression avec annulation par snackbar.
- Chaînes en anglais (`values/`) et en français (`values-fr/`) dès la première chaîne.
- Permissions déclarées : `INTERNET` et `ACCESS_NETWORK_STATE`.

**Terminé quand** on peut ajouter le PC du salon à la main et que les données survivent à un redémarrage de l'app.

### 4. Vue héros plein écran — `ui`

- Réécriture propre de la variante C du prototype. La référence visuelle et comportementale est la branche `prototype/power-button` (`PowerButtonVariants.kt`, fonction `VariantFullScreen`).
- Geste de maintien ~0,5 s :
  - la couleur se répand en cercle depuis le doigt ;
  - si on relâche avant, la couleur se retire et rien n'est envoyé.
- Haptique :

  | Moment | Retour |
  |---|---|
  | Début de l'appui | Tick |
  | Envoi | Confirmation |
  | Machine en ligne | Double confirmation |
  | Pas de réponse | Rejet |

- Mots d'état et couleurs : voir le tableau des états de la spec.
- Action TalkBack « Réveiller ».
- Permission `ACCESS_LOCAL_NETWORK` déclarée et demandée au premier réveil (Android 17+ : sans elle, chaque paquet échoue en `EPERM`).
- État vide : « Scanner un QR code » (désactivé jusqu'à l'étape 9) et « Saisir manuellement ».
- Test UI Compose, exécuté en JVM avec Robolectric : un relâchement avant 0,5 s n'envoie rien.
- La sonde TCP n'existe pas encore : après l'envoi, l'état reste `Booting` puis passe à `NoResponse`. Ça suffit à valider le réveil réel.

**Terminé quand** le PC du salon se réveille depuis l'app, sur un vrai téléphone.

### 5. Statut réel — `network`

- Sonde TCP sur les ports courants (445, 3389, 22, 5900, 548, plus 47989 et 47984 pour Sunshine/Moonlight), port mémorisé par machine dès qu'il répond. Repli sur `InetAddress.isReachable`.
- Résolution par hostname en priorité, puis par l'IP mémorisée.
- Sonde active uniquement quand un écran ou la tuile est visible.
- Avertissement léger si le sous-réseau du téléphone diffère de celui de la machine.
- État `Unknown` quand le téléphone n'est pas sur un réseau local.

### 6. Carousel et grille — `ui` + `data`

- Accueil en vue héros plein écran quel que soit le nombre de machines, en carousel horizontal avec des points en bas.
- Un doigt qui glisse annule le maintien : le réveil exige un doigt immobile pendant ~0,5 s. Test UI Compose : un swipe n'envoie rien.
- Ouverture sur la dernière machine consultée, mémorisée dans DataStore.
- Grille secondaire (sommaire) : états en direct, tap pour afficher la machine, appui long pour réordonner.
- Badges d'accès rapide (rond, triangle, carré arrondis via `graphics-shapes`) sur les 3 premières positions.
- Suppression du favori : l'ordre de la liste décide des accès rapides. Migration Room automatique (v2).
- Sonde toutes les 2 s, pour voir rapidement une machine qui s'éteint.

### 7. Tuiles Quick Settings et raccourcis d'app — `tile`

- 3 tuiles, pour les 3 premières machines de la liste. Un tap réveille la machine ; la tuile affiche le nom de la machine, son état en sous-titre et la forme de son badge comme icône.
- Raccourcis dynamiques d'app (appui long sur l'icône) pour les 3 premières machines, avec la forme et la couleur de leur badge, qui réveillent la machine d'un tap.
- Tuiles et raccourcis suivent l'ordre de la liste quand il change.

### 8. Notification de suivi

- Travail court (90 s max) via WorkManager expedited, ou un foreground service court si WorkManager ne suffit pas.
- Notification « démarre… » puis « allumé ✓ » ou « pas de réponse », qui disparaît seule.
- Permission `POST_NOTIFICATIONS` demandée au premier réveil ; repli silencieux si elle est refusée.
- Réglage pour désactiver la notification (DataStore).

### 9. Appairage par QR code

- **Côté PC**, script PowerShell `scripts/windows/pair.ps1` :
  - détection de l'interface active (Ethernet en priorité) ;
  - diagnostic WoL (`Get-NetAdapterPowerManagement`, démarrage rapide, Wi-Fi) avec correction opt-in ;
  - ouverture de `debout.sybers.fr/pair/#…`.
  - Tests Pester, exécutés en CI sous Windows PowerShell 5.1.
  - `debout.sybers.fr/win` sert un petit chargeur ASCII (`web/win`) qui télécharge `pair.ps1` et le décode en UTF-8 : GitHub Pages sert un fichier sans extension sans charset, que Windows PowerShell décoderait en ISO-8859-1.
- **Côté Mac et Linux**, scripts sh POSIX `scripts/macos/pair.sh` et `scripts/linux/pair.sh`, servis en `debout.sybers.fr/mac` et `/linux` pour `curl -fsSL … | sh` :
  - macOS : interface via `networksetup`, diagnostic `pmset womp` avec correction opt-in, nom affiché (`ComputerName`) et nom réseau (`LocalHostName.local`) ;
  - Linux : interface via `ip` et `/sys/class/net`, diagnostic `ethtool`, correction via NetworkManager si présent, sinon `ethtool -s` avec un avertissement sur la persistance ; QR dans le terminal avec `qrencode`, URL affichée sur un serveur sans navigateur ;
  - toutes les questions passent par `/dev/tty`, l'entrée standard étant le script lui-même ;
  - tests sh sans dépendance (`scripts/test.sh`), exécutés en CI sous `dash`, `bash` et le `sh` de macOS, plus ShellCheck.
- **Site** : page `/pair` qui génère le QR côté client à partir du fragment, et page d'accueil avec le choix de l'OS, la commande à copier et le lien vers la release. Le site est en anglais et en français.
- **App** : scan avec CameraX + ZXing (permission caméra au moment du scan), parsing de `debout://v1?…`, rejet propre des versions inconnues.

### 10. Aide au dépannage

- Checklist par OS affichée après un `NoResponse`, avec un lien vers la page d'aide équivalente du site.

### 11. Identité visuelle et première release

L'identité est décrite dans la section Design de la spec. Une sous-étape = un build vert et un commit.

1. **Fondations** : minSdk 33 (suppression des vérifications de version devenues inutiles), règles d'extraction de données pour la sauvegarde, spec et plan à jour.
2. **Thème** : palette color-block de la vue héros, thème Material généré depuis l'outremer avec surfaces grège, Unbounded et Recursive Mono embarquées, réglage « Couleurs du fond d'écran » (DataStore).
3. **Le personnage** : composant des yeux, expressions par état, animations au repos, maintien qui ouvre les yeux, figé sans animations système. Test UI Compose.
4. **Grain** : shader AGSL sur la vue héros, l'état vide et le fond du scan.
5. **Partout ailleurs** : badges sans couleur (~14 dp, en ligne devant le nom), mini-têtes de la grille, état vide, scan réussi, en-tête du dépannage.
6. **Icône** : icône adaptative avec calque monochrome, icônes des raccourcis, favicon et image de partage.
7. **Site** : palette, typographie et grain.
8. **Release** :
   - clé de signature créée par le propriétaire du dépôt (`keytool`, RSA 4096, alias `debout`), conservée dans un gestionnaire de mots de passe avec une copie hors ligne, et les 4 secrets posés avec `gh secret set` ;
   - `versionName` et `versionCode` écrits en dur, et la CI refuse un tag qui ne correspond pas au `versionName` ;
   - notes de version et fiche de l'app au format fastlane (`fastlane/metadata/android/{en-US,fr-FR}`), reprises par la release GitHub.

**Avant le tag `v0.1.0`** :

- [ ] identité implémentée (sous-étapes 2 à 7) ;
- [ ] CI verte : Android, Pester, scripts sh, ShellCheck ;
- [ ] DNS `debout.sybers.fr` en place, HTTPS forcé ;
- [ ] sur un vrai téléphone : réveil du PC du salon depuis l'app, une tuile et un raccourci ; appairage par QR avec le script Windows ; scripts macOS et Linux lancés au moins une fois ;
- [ ] clé et secrets posés, premier build de release signé réussi.

## Environnement

- **JDK** : celui d'Android Studio (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, JDK 25). **SDK** : `~/Library/Android/sdk` (API 37).
- **Commandes** : `cd android && ./gradlew assembleDebug lint testDebugUnitTest` (vérifications de la CI), `./gradlew installDebug` (appareil branché ou émulateur `Medium_Phone_API_37.0`).
- **Stack** : Gradle 9.8.2, AGP 9.5.0 (Kotlin intégré, pas de plugin `kotlin-android`), Kotlin 2.4.21, Compose BOM 2026.09.00.
- **Versions** : vérifier les dates de publication avant d'ajouter une dépendance (`maven-metadata.xml` sur `dl.google.com/android/maven2` ou Maven Central, puis l'en-tête `Last-Modified` du `.pom`).
- **Dépôt** : https://github.com/sybers/debout. GitHub Pages est actif avec le domaine `debout.sybers.fr`.
- **DNS en attente** : enregistrement `CNAME debout → sybers.github.io.` à ajouter chez le registrar, puis activer « Enforce HTTPS ».
