val stopButton: Button = findViewById(R.id.stopButton)

stopButton.setOnClickListener {
    stopReading() // Implement this function to handle stopping the reading
}

private fun stopReading() {
    // Logic to stop the reading process
    // This could involve stopping a thread, cancelling a task, etc.
}<Button
    android:id="@+id/stopButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Stop Reading" />pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "reader"
include(":app")
