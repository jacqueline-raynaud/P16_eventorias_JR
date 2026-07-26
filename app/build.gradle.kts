import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin)  plus utilisé depuis AGP 9 sinon plantage
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.gms.google.services)
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
            storeFile = file(getLocalProperty("storeFile"))
            storePassword = getLocalProperty("storePassword")
            keyAlias = getLocalProperty("keyAlias")
            keyPassword = getLocalProperty("keyPassword")
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
    // Core Android
    implementation("androidx.core:core-ktx:1.13.0")

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.firebase.config)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.appcompat)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle & Coroutines
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebaseui.auth)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)


    // Image Loading
    implementation(libs.coil.compose)

    // Testing - Unit Tests
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Testing - Instrumented Tests
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.mockk.android)

    // kotest
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)

    // reseau pour ne pas dépendre de coil
    implementation(libs.okhttp)
}

tasks.register<JacocoReport>("jacocoCombinedReport") {

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
            fileTree("${layout.buildDirectory.get()} {/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"){exclude(excludes)},
            fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes"){exclude(excludes)},
            fileTree(
                "${layout.buildDirectory.get()}/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
            ) {exclude(excludes)}
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

fun getLocalProperty(key: String): String {
    val localProperties = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localProperties.load(localFile.inputStream())
    }
    return localProperties.getProperty(key, "")
}

tasks.register("aggregateTestReportsHtml") {

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
