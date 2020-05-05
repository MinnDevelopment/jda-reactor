import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.3.21"
    id("com.jfrog.bintray") version "1.8.4"
}

group = "club.minnced"
version = "1.1.0"

repositories {
    jcenter()
}

dependencies {
    compileOnly("net.dv8tion:JDA:4.1.1_143")

    api("io.projectreactor:reactor-core:3.2.5.RELEASE")
    implementation(kotlin("stdlib"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val jar: Jar by tasks
val javadoc: Javadoc by tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


javadoc.apply {
    isFailOnError = false
    options.memberLevel = JavadocMemberLevel.PUBLIC
    options.encoding = "UTF-8"
    if (options is StandardJavadocDocletOptions) {
        val opt = options as StandardJavadocDocletOptions
        opt.author()
        opt.links(
            "https://projectreactor.io/docs/core/3.1.2.RELEASE/api/",
            "https://docs.oracle.com/javase/8/docs/api/",
            "https://ci.dv8tion.net/job/JDA/javadoc")
        if (JavaVersion.current().isJava9Compatible) {
            opt.addBooleanOption("html5", true)
            opt.addStringOption("-release", "8")
        }
    }
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
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            artifact(javadocJar)
            artifact(sourcesJar)
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

val build: Task by tasks
build.apply { 
    dependsOn(javadocJar)
    dependsOn(sourcesJar)
    dependsOn(jar)
}
