import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.4.32"
}

group = "club.minnced"
version = "1.3.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    compileOnly("net.dv8tion:JDA:4.2.1_258")

    api("io.projectreactor:reactor-core:3.3.15.RELEASE")
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

val build: Task by tasks
build.apply { 
    dependsOn(javadocJar)
    dependsOn(sourcesJar)
    dependsOn(jar)
}

publishing.publications {
    register("Release", MavenPublication::class) {
        from(components["java"])
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(javadocJar)
        artifact(sourcesJar)
    }
}