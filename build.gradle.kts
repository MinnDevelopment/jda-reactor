plugins {
    `java-library`
    kotlin("jvm") version "1.3.21"
}

group = "club.minnced"
version = "0.1.0"

sourceSets["test"].apply {
    compileClasspath += sourceSets["main"].compileClasspath
}

repositories {
    jcenter()
}

dependencies {
    compileOnly("net.dv8tion:JDA:4.ALPHA.0_35")
    compileOnly("io.projectreactor:reactor-core:3.2.5.RELEASE")
    compileOnly(kotlin("stdlib-jdk8"))

    testCompile("net.dv8tion:JDA:4.ALPHA.0_35")
    testCompile("io.projectreactor:reactor-core:3.2.5.RELEASE")
    testCompile(kotlin("stdlib-jdk8"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
