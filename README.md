# Eventorias

Projet OpenClassrooms
Application Android permettant de découvrir, créer et gérer des événements, avec authentification Firebase et notifications push.
2ème partie : github actions et SonarQ

## Fonctionnalités

- **Authentification** : email/mot de passe et Google (via FirebaseUI)
- **Liste des événements** : recherche, tri par date, filtre par catégorie
- **Détail d'un événement** : description, date/heure, lieu, carte statique Google Maps, avatar de l'organisateur
- **Création / édition d'événement** : ajout de photo via caméra ou galerie, géocodage automatique de l'adresse
- **Profil utilisateur** : édition du profil, avatar, activation/désactivation des notifications, suppression de compte
- **Notifications push** : un utilisateur reçoit une notification quand un nouvel événement est publié (si activé dans son profil)

## Stack technique

- **Langage / UI** : Kotlin, Jetpack Compose
- **Architecture** : MVVM (UiState / Action / Effect), injection de dépendances avec **Hilt**
- **Navigation** : Navigation Compose
- **Asynchrone** : Coroutines & Flow
- **Images** : Coil (chargement asynchrone)
- **Firebase** : Authentication, Firestore, Storage, Cloud Messaging, Cloud Functions (Node.js/TypeScript)
- **Cartes** : Google Maps Static API
- **Tests** : JUnit5, Kotest, MockK, Compose UI Test / Espresso, Jacoco (couverture)

`minSdk` 26 · `compileSdk`/`targetSdk` 36

## Structure du projet

```
app/src/main/java/.../
├── data/            # Repositories, accès Firebase (Firestore, Storage, Auth, Messaging)
├── domain/          # Modèles métier, UseCases
├── presentation/    # Écrans Compose, ViewModels, navigation
├── di/              # Modules Hilt
└── service/         # Service de réception des notifications FCM

functions/           # Cloud Function (TypeScript) : notifyOnNewEvent
```

## Configuration requise avant de lancer le projet

1. **`google-services.json`** : à placer dans `app/`, généré depuis la console Firebase du projet.
2. **`local.properties`** : ajouter la clé de l'API Google Maps Static :
   ```
   google.maps.key=CLE_API
   ```
3. **Cloud Functions** : déployées séparément depuis `functions/` (`firebase deploy --only functions`).

## Base de données (Firestore)

Deux collections principales :

- **`events`** : `id`, `name`, `description`, `date`, `locationName`, `location` (GeoPoint), `category`, `imageUrl`, `organizerId`, `guests`
- **`users`** : `uid`, `firstName`, `lastName`, `email`, `avatarUrl`, `notificationEnabled`, `fcmToken`

## Sécurité

Règles Firestore/Storage : lecture des événements publique, écriture réservée à l'organisateur ; profils utilisateurs lisibles par tout utilisateur connecté, modifiables uniquement par leur propriétaire.

## Notifications

À la création d'un événement, une Cloud Function (`functions/src/index.ts`) est déclenchée automatiquement, recherche les utilisateurs ayant activé les notifications, et leur envoie une notification push via Firebase Cloud Messaging.

## Tests

```
./gradlew test                      # tests unitaires
./gradlew connectedAndroidTest       # tests instrumentés (émulateur ou appareil requis)
```
