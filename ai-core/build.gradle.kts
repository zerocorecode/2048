plugins { id("org.jetbrains.kotlin.jvm") }

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    implementation(project(":game-core"))
    testImplementation(kotlin("test"))
}
tasks.test { useJUnitPlatform() }
