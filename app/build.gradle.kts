import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin)  plus utilisé depuis AGP 9 sinon plantage
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.sonarqube)
    jacoco
}

android {
    namespace = "fr.quinquenaire.p15_eventorias_jr"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.quinquenaire.eventorias"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "fr.quinquenaire.p15_eventorias_jr.CustomTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "GOOGLE_MAPS_API_KEY",
            "\"${getLocalProperty("google.maps.key")}\""
        )
    }
    signingConfigs {
        create("release") {
            val keystorePath = getLocalProperty("storeFile")
            if (keystorePath != null && keystorePath.toString().isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = getLocalProperty("storePassword")
                keyAlias = getLocalProperty("keyAlias")
                keyPassword = getLocalProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
// implementation
    // Core Android & Appcompat
    implementation(libs.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Lifecycle & Coroutines
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coroutines.core)

    // Firebase & Auth
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebaseui.auth)
    implementation(libs.firebase.config)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Utilitaires (Image, Réseau, Règles)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.androidx.rules)

// outils
    ksp(libs.hilt.compiler)

// debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

// tests unitaires
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)

// tests instrumentés
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)

    kspAndroidTest(libs.hilt.compiler)
}

tasks.register<JacocoReport>("jacocoCombinedReport") {
    description = "Generates xml coverage report for this project."
    group = JavaBasePlugin.VERIFICATION_GROUP

    dependsOn(
        "testDebugUnitTest",
        "connectedDebugAndroidTest"
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test.class",
        "**/*Test$*.class",
        "**/di/**",
        "**/*Module*",
        "**/*_HiltComponents*",
        "**/*_Hilt*",
        "**/Hilt_*",
        "**/*_Factory*",
        "**/*_MembersInjector*",
        "**/*_GeneratedInjector*",
        "**/*_ComponentTreeDeps*",
        "**/hilt_aggregated_deps/**",
        "**/dagger/hilt/**"
    )

    classDirectories.setFrom(
        files(
            fileTree("${layout.buildDirectory.get()} {/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
                exclude(
                    excludes
                )
            },
            fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
                exclude(
                    excludes
                )
            },
            fileTree(
                "${layout.buildDirectory.get()}/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
            ) { exclude(excludes) }
        )
    )

    sourceDirectories.setFrom(
        files(
            "$projectDir/src/main/java",
            "$projectDir/src/main/kotlin"
        )
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
            include("outputs/code_coverage/debugAndroidTest/**/*.ec")
        }
    )
}

tasks.register("aggregateTestReportsHtml") {
    description = "Generates xml coverage report for this project."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    dependsOn(
        "testDebugUnitTest",
        "connectedDebugAndroidTest"
    )

    doLast {

        val reportDir = layout.buildDirectory.dir("reports/allTests").get().asFile

        reportDir.mkdirs()

        File(reportDir, "index.html").writeText(
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Rapports de tests</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 40px;
                    }
                    h1 {
                        color: #333;
                    }
                    ul {
                        line-height: 2;
                    }
                </style>
            </head>
            <body>
                <h1>Rapports de tests Eventorias</h1>

                <ul>
                    <li>
                        <a href="../tests/testDebugUnitTest/index.html">
                            Rapport des tests unitaires
                        </a>
                    </li>

                    <li>
                        <a href="../androidTests/connected/index.html">
                            Rapport des tests instrumentés Android
                        </a>
                    </li>
                </ul>

            </body>
            </html>
            """.trimIndent()
        )

        println("Rapport agrégé : ${reportDir.absolutePath}/index.html")
    }
}
fun getLocalProperty(key: String): String {
    val localProperties = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localProperties.load(localFile.inputStream())
    }
    return localProperties.getProperty(key, "")
}

sonar {
    properties {
        property("sonar.projectKey", "jacqueline-raynaud_P16_eventorias_JR")
        property("sonar.organization", "jacqueline-raynaud")
        property("sonar.projectName", "P16_Eventorias")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.qualitygate.wait", "true")
        property("sonar.androidLint.reportPaths", "")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/jacocoCombinedReport/jacocoCombinedReport.xml,${layout.buildDirectory.get()}/reports/coverage/androidTest/debug/connected/report.xml"
        )
        property(
            "sonar.coverage.exclusions",
            "**/theme/**, **/*Module.kt, **/AndroidGeocoderManager.kt"
        )

    }
}
