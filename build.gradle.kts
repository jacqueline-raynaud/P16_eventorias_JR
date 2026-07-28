// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.sonarqube)
}
sonar {
    properties {
        property("sonar.projectKey", "jacqueline-raynaud_P16_eventorias_JR")
        property("sonar.organization", "jacqueline-raynaud")
        property("sonar.androidLint.reportPaths", "")
        property("sonar.coverage.jacoco.xmlReportPaths", "")
    }
}
subprojects {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                "build/reports/jacoco/jacocoCombinedReport/jacocoCombinedReport.xml,build/reports/coverage/androidTest/debug/connected/report.xml"
            )
        }
    }
}