// AGP built-in Kotlin: explicitly retain the same fixed KGP/compiler version.
buildscript {
    dependencies { classpath(libs.kotlin.gradle.plugin) }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
