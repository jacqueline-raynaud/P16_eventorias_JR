# Eventorias

Projet OpenClassrooms
Application Android permettant de découvrir, créer et gérer des événements, avec authentification Firebase et notifications push.
il s'agit de la deuxième partie du projet : qui consiste à mettre en place 

# Eventorias

<!-- Remplacer "VOTRE_NOM" et "VOTRE_REPO" par vos vraies informations pour que les badges fonctionnent -->
![CI Status](https://github.com/jacqueline-raynaud/P16_eventorias_JR/actions/workflows/android-ci.yml/badge.svg)
![CD Status](https://github.com/jacqueline-raynaud/P16_eventorias_JR/actions/workflows/android-cd.yml/badge.svg)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=jacqueline-raynaud_P16_eventorias_JR&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jacqueline-raynaud_P16_eventorias_JR)

Projet OpenClassrooms : Application Android permettant de découvrir, créer et gérer des événements.
**Cette version intègre un pipeline complet d'Intégration et de Déploiement Continus (CI/CD) ainsi qu'un contrôle strict de la qualité du code.**

## Fonctionnalités de l'application

- **Authentification** : email/mot de passe et Google (via FirebaseUI).
- **Liste des événements** : recherche, tri par date, filtre par catégorie.
- **Détail d'un événement** : description, date/heure, lieu, carte statique Google Maps, avatar de l'organisateur.
- **Création / édition d'événement** : ajout de photo via caméra ou galerie, géocodage automatique de l'adresse.
- **Profil utilisateur** : édition du profil, avatar, gestion des notifications, suppression de compte.
- **Notifications push** : alerte à la publication d'un nouvel événement.

## Stack technique & DevOps

- **Langage / UI** : Kotlin, Jetpack Compose
- **Architecture** : MVVM, injection de dépendances avec **Hilt**
- **Firebase** : Auth, Firestore, Storage, Cloud Messaging, Functions
- **DevOps & CI/CD** : GitHub Actions, Firebase App Distribution
- **Qualité & Couverture** : SonarCloud, JaCoCo
- **Tests** : JUnit5, Kotest, MockK, Compose UI Test / Espresso

`minSdk` 26 · `compileSdk`/`targetSdk` 36

## Automatisation CI/CD (GitHub Actions)

Le projet utilise des workflows automatisés pour garantir la fiabilité de chaque livraison.

### 1. Intégration Continue (CI) - `android-ci.yml`
Déclenché à chaque **Pull Request** vers la branche `main` :
- Vérification du code et exécution des tests unitaires (`./gradlew test`).
- Lancement des tests instrumentés sur un émulateur headless (`./gradlew connectedAndroidTest`).
- Génération du rapport de couverture de code (JaCoCo).
- Analyse statique et validation de la *Quality Gate* via **SonarCloud**. Le merge est bloqué si les critères de qualité ne sont pas respectés.

### 2. Déploiement Continu (CD) - `android-cd.yml`
Déclenché lors de la création d'un **Tag** (ex: `v1.0.0`) :
- Compilation de l'APK en mode `Release`.
- Signature sécurisée de l'application via le Keystore.
- Déploiement automatique vers **Firebase App Distribution** pour alerter les testeurs de la disponibilité d'une nouvelle version.

## Configuration requise (Secrets)

Pour reproduire l'environnement CI/CD sur un fork de ce dépôt, les secrets suivants doivent être configurés dans les paramètres GitHub (`Settings > Secrets and variables > Actions`) :

- `GOOGLE_SERVICES_JSON` : Le contenu du fichier `google-services.json` encodé en Base64.
- `MAPS_API_KEY` : La clé d'API Google Maps.
- `KEYSTORE_B64` : Le fichier `release.keystore` encodé en Base64.
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` : Identifiants de signature de l'APK.
- `SONAR_TOKEN` : Token d'authentification pour SonarCloud.

*Note : Les fichiers sensibles (JSON, Keystore, local.properties) sont strictement ignorés par Git via le `.gitignore`.*

## 🗄 Base de données (Firestore)

Deux collections principales :
- **`events`** : `id`, `name`, `description`, `date`, `locationName`, `location` (GeoPoint), `category`, `imageUrl`, `organizerId`, `guests`
- **`users`** : `uid`, `firstName`, `lastName`, `email`, `avatarUrl`, `notificationEnabled`, `fcmToken`

Règles de sécurité : lecture publique des événements, écriture réservée à l'organisateur. Profils utilisateurs lisibles par tous les connectés, modifiables uniquement par leur propriétaire.

## 🧪 Lancer les tests en local

```bash
# Tests unitaires
./gradlew test                      

# Tests instrumentés (émulateur ou appareil physique requis)
./gradlew connectedAndroidTest

# Tests avec Jacococ
./gradlew JacocoReport

# Combaison tests instrumentés et tests unitaires sur un fichier html
/.gradlew aggregateTestReportsHtml
