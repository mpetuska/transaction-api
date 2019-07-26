import org.jetbrains.kotlin.gradle.tasks.*

val ktorVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val hikariCPVersion: String by project
val h2Version: String by project
val kodeinVersion: String by project
val spekVersion: String by project
val mockkVersion: String by project
val mainClassName = "io.ktor.server.netty.EngineMain"

plugins {
  kotlin("jvm") version "1.3.41"
  idea
}

group = "lt.petuska"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenLocal()
  jcenter()
  maven { url = uri("https://kotlin.bintray.com/ktor") }
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-json:$ktorVersion")
  implementation("io.ktor:ktor-client-gson:$ktorVersion")
  implementation("io.ktor:ktor-gson:$ktorVersion")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  implementation("org.jetbrains.exposed:exposed:$exposedVersion")
  implementation("com.zaxxer:HikariCP:$hikariCPVersion")
  implementation("com.h2database:h2:$h2Version")
  implementation("org.kodein.di:kodein-di-generic-jvm:$kodeinVersion")
  implementation("org.kodein.di:kodein-di-framework-ktor-server-jvm:$kodeinVersion")

  testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
  testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
  testImplementation("io.mockk:mockk:$mockkVersion")
  testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
}

kotlin {
  sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
  sourceSets["test"].kotlin.srcDirs("src/test/kotlin")
}

sourceSets["main"].resources.srcDirs("src/main/resources")
sourceSets["test"].resources.srcDirs("src/test/resources")

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }
  getByName("test", Test::class) {
    useJUnitPlatform {
      includeEngines("spek2")
    }
  }
  getByName("jar", Jar::class) {
    manifest {
      attributes(
        mapOf(
          "Implementation-Title" to rootProject.name,
          "Implementation-Group" to rootProject.group,
          "Implementation-Version" to rootProject.version,
          "Timestamp" to System.currentTimeMillis(),
          "Main-Class" to mainClassName
        )
      )
    }
    val dependencies = configurations["runtimeClasspath"].filter { it.name.endsWith(".jar") }
    dependencies.forEach {
      if (it.isDirectory) from(it) else from(zipTree(it))
    }
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    inputs.files(dependencies)
    outputs.file(archiveFile)
  }
  create("run", JavaExec::class) {
    dependsOn("classes", "processResources")
    group = "run"
    workingDir = buildDir
    classpath = sourceSets["main"].runtimeClasspath +
        project.tasks["processResources"].outputs.files
    main = mainClassName
  }
}
