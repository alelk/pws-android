plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("maven-publish")
}

kotlin {
  jvm()
  sourceSets {
    val commonMain by getting {
      dependencies {
      }
    }
    val jvmMain by getting {}

    val commonTest by getting {
      dependencies {
        implementation(project(":domain:domain-test-fixtures"))
        implementation(libs.kotest.property)
        implementation(libs.kotest.framework.engine)
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(libs.kotest.runner.junit5)
      }
    }
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

publishing {
  val versionName = rootProject.extra["versionName"] as String
  val isSnapshot by lazy { versionName.endsWith("SNAPSHOT") }

  publications {
    create<MavenPublication>("gpr") {
      groupId = "io.github.alelk.pws"
      artifactId = "pws-domain"
      version = versionName
      artifact(tasks.named("jvmJar"))
      artifact(tasks.named("jvmSourcesJar"))
    }
  }

  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/alelk/pws-android")
      credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER") ?: "alelk"
        password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
      }
    }
  }
}