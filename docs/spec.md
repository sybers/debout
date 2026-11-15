# Debout — Spécification produit V1

## Vision

Debout est une app Android de Wake-on-LAN belle, ergonomique, gratuite et open source.

Elle est construite d'abord pour un besoin concret : allumer facilement le PC Windows du salon depuis n'importe quelle pièce de la maison. À terme, elle vise un public technique (selfhosters, homelab) mal servi par les apps existantes, souvent laides, peu ergonomiques ou payantes.

Les besoins avancés de ce public ne doivent pas alourdir la V1.

## Principes

- **Rapide** : réveiller une machine prend un seul geste, quel que soit le nombre de machines.
- **Honnête** : l'utilisateur sait toujours si le réveil a fonctionné.
- **Sobre en permissions** : aucune permission intrusive (pas de localisation).
- **Libre** : pas de pub, pas de dépendance propriétaire, code vérifiable.

## Périmètre réseau

- Réveil sur le **réseau local uniquement** en V1.
- Envoi de **3 magic packets** (6 × `FF` + 16 × MAC) en broadcast UDP, espacés de ~100 ms.
- Une machine est modélisée par une MAC, une adresse de destination et un port, pour que le réveil à distance (hôte public, VPN) puisse s'ajouter sans refonte.

### Hors réseau local

- Si le téléphone n'est connecté ni au Wi-Fi ni à l'Ethernet, l'app et la tuile affichent « Pas sur un réseau local » et le bouton est désactivé.
- Si le sous-réseau du téléphone diffère de celui de la machine, un avertissement léger s'affiche (« Cette machine semble être sur un autre réseau »), sans bloquer l'envoi. Le SSID n'est jamais lu : ça exigerait la permission de localisation.

## Statut des machines

- Le statut en ligne/hors ligne est vérifié en continu tant que l'app ou la tuile est visible. Aucun service ne tourne en arrière-plan en dehors du suivi d'un réveil.
- Le check se fait par connexion TCP. L'app essaie une liste de ports courants (445, 3389, 22, 5900, 548) et mémorise celui qui répond pour chaque machine. Le port reste modifiable dans les réglages avancés. `InetAddress.isReachable` sert de repli.
- La machine est résolue par son hostname en priorité (NetBIOS/mDNS), puis par l'IP mémorisée, pour supporter le DHCP.

### États

| État | Signification | Mot affiché (vue héros) | Rendu |
|---|---|---|---|
| Hors ligne | La machine ne répond pas | « Endormi » | Fond sombre et froid |
| Envoi | Paquets en cours d'émission | « Hé ! » | Transition brève, haptique net |
| Démarrage… | Paquets envoyés, en attente de réponse | « Réveil… » | Fond ambre sombre qui respire, temps écoulé discret |
| En ligne | La machine répond | « Debout ! » | Fond ambre plein, haptique double tick |
| Pas de réponse | Aucune réponse après ~90 s | « Silence radio » | Fond éteint, haptique de rejet, maintien pour réessayer, accès à l'aide |
| Inconnu | Statut impossible à déterminer (pas d'IP, hors réseau local) | « Hors réseau » | Fond neutre, distinct de « Hors ligne », maintien désactivé |

Une demande de réveil sur une machine déjà en ligne n'envoie rien et affiche « Déjà allumé ».

### Transitions

| Depuis | Événement | Vers |
|---|---|---|
| Tout état | Le téléphone quitte le réseau local | Inconnu (l'envoi et l'attente en cours sont annulés) |
| Tout état | La sonde répond | En ligne (l'envoi et l'attente en cours sont annulés) |
| Inconnu, En ligne | La sonde ne répond pas | Hors ligne |
| Envoi, Démarrage…, Pas de réponse | La sonde ne répond pas | Inchangé |
| Hors ligne, Pas de réponse | Demande de réveil | Envoi |
| En ligne | Demande de réveil | Inchangé, « Déjà allumé » |
| Inconnu, Envoi, Démarrage… | Demande de réveil | Inchangé, refusée |
| Envoi | Dernier des 3 paquets envoyé | Démarrage… |
| Démarrage… | ~90 s sans réponse | Pas de réponse |

### Notification de suivi

Après un réveil, une notification temporaire suit la progression (« Mon-PC démarre… » → « Mon-PC est allumé ✓ » ou « Pas de réponse ») et disparaît seule après quelques secondes. Elle s'appuie sur un travail court, limité à 90 s.

- Activée par défaut, désactivable dans les réglages.
- La permission notifications (Android 13+) est demandée au premier réveil. Si elle est refusée, le suivi se fait silencieusement dans l'app.

## Points d'entrée

| Point d'entrée | Geste | Cible |
|---|---|---|
| App | Appui maintenu ~0,5 s | La machine choisie |
| Tuile Quick Settings | Tap | La machine favorite |
| Raccourci d'app (appui long sur l'icône) | Tap | Une machine |

### Appui maintenu dans l'app

- Dans la vue héros, tout l'écran est la zone de maintien. Pendant l'appui, la couleur se répand en cercle depuis le doigt. Au terme des ~0,5 s, le réveil part avec un retour haptique.
- Sur une carte de la grille, le bouton de la carte se remplit de la même manière, en format compact.
- Si l'utilisateur relâche avant, la couleur se retire en douceur et rien n'est envoyé.
- Le geste évite les déclenchements accidentels, notamment en faisant défiler la grille.
- Les éléments de navigation (retour, réglages) restent au-dessus de la zone de maintien et ne la déclenchent pas.
- TalkBack expose une action « Réveiller » standard.

La tuile et le raccourci gardent un tap simple : ce sont déjà des actions délibérées en deux temps, et Android réserve l'appui long sur une tuile à l'ouverture de l'app. La tuile affiche les états de la machine favorite.

## Écrans

### Accueil

- **1 machine** : vue héros plein écran. Tout l'écran est le bouton ; le nom de la machine s'affiche en petit, l'état en très gros mot, et une ligne d'indication en dessous (« Maintiens n'importe où », temps écoulé, aide).
- **2 machines ou plus** : grille de cartes. Chaque carte affiche le nom, l'icône de type, le statut et son propre bouton de réveil, actionnable directement. Un tap sur le reste de la carte ouvre la vue héros plein écran de la machine.

### Premier lancement

Pas de carrousel d'onboarding. L'état vide guide vers l'appairage :

- une illustration ;
- un bouton principal **« Scanner un QR code »** ;
- une ligne d'explication : « Sur ton PC, ouvre debout.sybers.fr » ;
- un lien secondaire « Saisir manuellement ».

### Aide au dépannage

Après un échec (« Pas de réponse »), l'app propose une checklist courte par OS :

- WoL activé dans le BIOS/UEFI ;
- options d'alimentation de la carte réseau (sortie de veille, magic packet) ;
- démarrage rapide de Windows désactivé ;
- machine branchée en Ethernet plutôt qu'en Wi-Fi ;
- sur macOS : « Réveiller pour l'accès au réseau » activé. Un Mac se réveille depuis la veille, pas depuis un arrêt complet.

La checklist renvoie vers la page d'aide équivalente du site.

## Machines

### Données

| Champ | Visibilité | Source QR |
|---|---|---|
| Nom | Standard | Hostname, modifiable |
| Type / icône (PC fixe, portable, serveur, NAS, Mac, autre) | Standard | Deviné depuis l'OS |
| Adresse MAC | Standard | Oui |
| IP | Standard | Oui |
| Hostname | Standard | Oui |
| Favori (pilote la tuile) | Standard | Non (première machine par défaut) |
| Adresse de broadcast | Avancé | Calculée |
| Port WoL (9 par défaut) | Avancé | Non |
| Port de check du statut | Avancé | Auto-détecté |

### Gestion

- Modifier une machine.
- Supprimer, avec annulation via un snackbar plutôt qu'une confirmation.
- Réordonner par glisser-déposer.

### Ajout

**Saisie manuelle** : le champ MAC accepte `AA:BB:CC:DD:EE:FF`, `aa-bb-cc-dd-ee-ff` et `aabbccddeeff`, ainsi que le collage. Une aide « Où trouver ma MAC ? » est proposée par OS.

**Appairage par QR code** : voir la section suivante. Le scan utilise CameraX + ZXing et demande la permission caméra au moment du scan.

## Appairage par QR code

Un site web ne peut pas lire la MAC de la machine qui le visite : un script local collecte les informations.

### Parcours

1. Sur le PC, l'utilisateur ouvre `debout.sybers.fr`, choisit son OS et copie la commande (Windows : `irm debout.sybers.fr/win | iex`).
2. Le script détecte l'interface réseau active, en privilégiant l'Ethernet, et récupère la MAC, l'IP et le hostname.
3. Le script vérifie la configuration WoL et signale ce qui cloche :
   - options d'alimentation de la carte réseau (`Get-NetAdapterPowerManagement`) ;
   - démarrage rapide de Windows ;
   - interface Wi-Fi plutôt qu'Ethernet ;
   - rappel d'activer le WoL dans le BIOS/UEFI, non détectable.
4. Pour les points corrigeables, le script affiche ce qu'il va modifier et demande une confirmation explicite avant d'agir, avec élévation admin.
5. Le script ouvre le navigateur sur `https://debout.sybers.fr/pair#…`. Les données sont dans le fragment : elles ne sont jamais envoyées au serveur.
6. La page génère le QR code côté client.
7. Dans l'app : « Scanner un QR code » → la machine est ajoutée, tous les champs pré-remplis.

### Format du payload

URI versionnée : `debout://v1?mac=…&ip=…&name=…&os=…`. L'app rejette proprement une version qu'elle ne connaît pas.

### Site

- Statique, sans backend ni étape de build : HTML/JS vanilla + une bibliothèque QR.
- Hébergé sur GitHub Pages, sur `debout.sybers.fr` (enregistrement CNAME).
- Pages : accueil (choix de l'OS, commande à copier, lien vers la dernière release de l'app, lien vers le code source du script), `/pair` (affichage du QR), aide au dépannage.
- Les scripts sont servis depuis le même domaine et versionnés dans le dépôt.

## Design

- **Material 3** pour la structure, les composants et les comportements attendus.
- **Identité propre** : palette, typographie et un composant signature, la vue héros plein écran et ses états animés.
- Micro-animations et retours haptiques soignés.
- Mode sombre de premier ordre.
- Couleurs dynamiques (Material You) en option dans les réglages.

## Langues

- Anglais et français, pour l'app, le site et la sortie des scripts.
- L'app suit la langue du système.
- Le nom « Debout » ne se traduit pas.

## Sauvegarde

Android Auto Backup : les machines et les préférences sont restaurées automatiquement lors d'un changement de téléphone.

## Stack technique

| Sujet | Choix |
|---|---|
| Langage / UI | Kotlin, Jetpack Compose, Material 3 |
| minSdk | 26 |
| Persistance | Room (machines), DataStore (préférences) |
| Scan QR | CameraX + ZXing (pas de Play Services) |
| Suivi du réveil | WorkManager expedited ou foreground service court |
| Package | `fr.sybers.debout` |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` (au scan), `POST_NOTIFICATIONS` (au premier réveil) |

Le projet n'embarque aucune dépendance propriétaire, pour rester compatible F-Droid.

## Dépôt

Monorepo GitHub public, sous licence **GPL-3.0** :

```
/android   l'application
/web       le site statique
/scripts   les scripts d'appairage
/docs      la documentation produit
```

## Qualité

### CI (GitHub Actions)

- Sur chaque PR : lint et tests.
- Sur chaque tag : build de l'APK signé, publié sur GitHub Releases.

### Tests

- Construction du magic packet et parsing tolérant de la MAC.
- Calcul de l'adresse de broadcast.
- Parsing et validation du payload QR, y compris les versions inconnues.
- Machine à états du réveil : les 6 états, les timeouts, les transitions.
- Tests UI Compose du bouton maintenu : un relâchement anticipé n'envoie rien.
- Tests Pester du script Windows.

## Distribution

APK signé sur GitHub Releases, installable directement ou mis à jour via Obtainium.

## Feuille de route

| Version | Contenu |
|---|---|
| V1.1 | Widget écran d'accueil (Glance), export/import JSON, partage d'une machine par QR |
| V1.x | Scripts d'appairage macOS et Linux, découverte réseau (NetBIOS/mDNS), publication F-Droid et Play Store |
| Plus tard | Réveil à distance (hôte public, VPN), traductions communautaires (Weblate) |

Hors périmètre : assistant vocal, groupes et tags, couleur par machine, réveil de plusieurs machines d'un coup.

Avant la publication sur les stores : vérifier les collisions du nom « Debout » sur le Play Store, F-Droid et GitHub. La publication Play Store d'un nouveau compte personnel impose un test fermé de 12 testeurs pendant 14 jours.
