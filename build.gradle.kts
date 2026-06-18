// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.8.0")
    }

    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint("1.8.0")
    }

    format("misc") {
        target(
            ".editorconfig",
            ".gitignore",
            "*.properties",
            "*.md",
            ".github/**/*.yml",
            ".github/**/*.yaml"
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
