plugins {
    java
    id("net.neoforged.moddev") version "2.0.144"
}

group = property("mod_group_id") as String
// Jar name carries the Minecraft version and a v-prefixed mod version so the
// two nodes never collide (riftgun-1.21.1-v0.1.0-rc.1.jar vs
// riftgun-26.1.2-v0.1.0-beta.6.jar); mods.toml still expands the bare mod_version.
version = "${property("minecraft_version")}-v${property("mod_version")}"

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
    mods {
        create("riftgun") {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        create("client") {
            client()
            systemProperty("riftgun.guiCapture", System.getProperty("riftgun.guiCapture", "false"))
            loadedMods.set(mods)
        }
        create("server") {
            server()
            loadedMods.set(mods)
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
        // Node resources shadow shared ones (e.g. version-specific item
        // models), so mount them first and let processResources exclude
        // duplicates in favour of the node copy.
        resources.setSrcDirs(listOf(file("versions/$node/src/main/resources")) + resources.srcDirs)
    }
}

dependencies {
    // Optional client integrations are never bundled. Each node declares its
    // own versions; nodes without the properties (e.g. 26.1.2 until JEI/
    // RyoamicLights ship builds) simply skip the dependency.
    val ryoamicProject = findProperty("ryoamiclights_modrinth_project_id") as String?
    val ryoamicVersion = findProperty("ryoamiclights_modrinth_version_id") as String?
    if (ryoamicProject != null && ryoamicVersion != null) {
        optionalClientCompileOnly("maven.modrinth:$ryoamicProject:$ryoamicVersion")
    }
    val immersivePortalsVersion = findProperty("immersive_portals_version") as String?
    if (immersivePortalsVersion != null) {
        optionalClientCompileOnly(
            "maven.modrinth:immersive-portals-neoforge:$immersivePortalsVersion"
        )
    }
    val jeiVersion = findProperty("jei_version") as String?
    if (jeiVersion != null) {
        val jeiApi = "mezz.jei:jei-${property("minecraft_version")}-neoforge-api:$jeiVersion"
        val jeiFull = "mezz.jei:jei-${property("minecraft_version")}-neoforge:$jeiVersion"
        // Present only in development runs.
        optionalClientCompileOnly(jeiApi)
        optionalClientRuntimeOnly(jeiFull)
        // The JEI bridge and its registration tests compile against the API.
        testImplementation(jeiApi)
    }
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
    // Node resource dirs shadow shared ones per version (see sourceSets above).
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
