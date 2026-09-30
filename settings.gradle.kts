pluginManagement { repositories { google(); maven { url = uri(System.getenv("MAVEN_CENTRAL_MIRROR") ?: "https://repo.maven.apache.org/maven2") }; gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); maven { url = uri(System.getenv("MAVEN_CENTRAL_MIRROR") ?: "https://repo.maven.apache.org/maven2") } } }
rootProject.name = "GottDiagnostics"
include(":app")
