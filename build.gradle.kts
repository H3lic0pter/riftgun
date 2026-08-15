plugins {
    java
    id("net.neoforged.moddev") version "2.0.144"
}

group = property("mod_group_id") as String
version = property("mod_version") as String

base {
    archivesName = property("mod_id") as String
}

// Each node declares its own JDK via java_version in versions/<node>/gradle.properties.
java.toolchain.languageVersion = JavaLanguageVersion.of(property("java_version") as String)

repositories {
    mavenCentral()
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }
    maven {
        name = "Jared Maven"
        url = uri("https://maven.blamejared.com")
        content { includeGroup("mezz.jei") }
    }
}

val optionalClientCompileOnly = configurations.create("optionalClientCompileOnly")
val optionalClientRuntimeOnly = configurations.create("optionalClientRuntimeOnly")
configurations.compileOnly.get().extendsFrom(optionalClientCompileOnly)
configurations.runtimeOnly.get().extendsFrom(optionalClientRuntimeOnly)
// ModDevGradle wires Minecraft/NeoForge into the main source set only; tests that
// touch Minecraft types need the same dependencies on their compile and runtime paths.
configurations.configureEach {
    if (name == "modDevCompileDependencies") {
        configurations.testCompileClasspath.get().extendsFrom(this)
    }
    if (name == "modDevRuntimeDependencies") {
        configurations.testRuntimeClasspath.get().extendsFrom(this)
    }
}

neoForge {
    version = property("neoforge_version") as String
    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }
}

// Per-version private sources (versions/<node>/src): same-FQCN files here
// override the shared tree. Only mounted when the directory exists.
val node = sc.current.project
sourceSets.main {
    if (file("versions/$node/src/main/java").isDirectory) {
        java.srcDir("versions/$node/src/main/java")
    }
    if (file("versions/$node/src/main/resources").isDirectory) {
        resources.srcDir("versions/$node/src/main/resources")
    }
}

dependencies {
    // Present only in development runs; optional client integrations are never bundled.
    optionalClientCompileOnly(
        "maven.modrinth:${property("ryoamiclights_modrinth_project_id")}:${property("ryoamiclights_modrinth_version_id")}"
    )
    optionalClientCompileOnly("mezz.jei:jei-1.21.1-neoforge-api:${property("jei_version")}")
    optionalClientRuntimeOnly("mezz.jei:jei-1.21.1-neoforge:${property("jei_version")}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Tests reference root-relative paths (src/main/java, src/main/resources);
    // node builds run from versions/<node>/, so anchor the working dir at the tree root.
    workingDir(rootProject.projectDir)
}

tasks.jar {
    // Stonecutter nodes run this script from versions/<node>/; the LICENSE lives at the tree root.
    from(rootProject.layout.projectDirectory.file("LICENSE")) { into("META-INF") }
}

// NeoGradle expanded these placeholders in mods.toml; ModDevGradle does not.
tasks.processResources {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "mod_id" to project.property("mod_id") as String,
            "mod_version" to project.property("mod_version") as String,
            "mod_name" to project.property("mod_name") as String,
            "neoforge_version" to project.property("neoforge_version") as String,
            "minecraft_version" to project.property("minecraft_version") as String,
        )
    }
}
