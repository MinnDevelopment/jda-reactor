import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.3.21"
    id("com.jfrog.bintray") version "1.8.4"
}

group = "club.minnced"
version = "0.1.2"

repositories {
    jcenter()
}

dependencies {
    compileOnly("net.dv8tion:JDA:4.ALPHA.0_35")
    compileOnly("io.projectreactor:reactor-core:3.2.5.RELEASE")
    implementation(kotlin("stdlib"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val jar: Jar by tasks
val javadoc: Javadoc by tasks
val compileKotlin: KotlinCompile by tasks
compileKotlin.apply {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar = task<Jar>("sourcesJar") {
    from(sourceSets["main"].allSource)
    classifier = "sources"
}

val javadocJar = task<Jar>("javadocJar") {
    from(javadoc.destinationDir)
    classifier = "javadoc"

    dependsOn(javadoc)
}

publishing {
    publications {
        register("BintrayRelease", MavenPublication::class) {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            artifact(javadocJar)
            artifact(sourcesJar)
            artifact(jar)
        }
    }
}

bintray {
    setPublications("BintrayRelease")
    user = properties["bintrayName"] as? String ?: ""
    key  = properties["bintrayKey"] as? String ?: ""
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        setLicenses("Apache-2.0")
        repo = "maven"
        vcsUrl = "https://github.com/MinnDevelopment/jda-reactor"
        githubRepo = "minndevelopment/jda-reactor"
        issueTrackerUrl = "$vcsUrl/issues"
        websiteUrl = vcsUrl
        desc = "A collection of kotlin extensions for JDA that make use with reactor-core easier."
        setLabels("reactive", "jda", "discord", "kotlin")
        name = project.name
        publish = true
        publicDownloadNumbers = true
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version as String
            gpg.sign = true
        })
    })
}