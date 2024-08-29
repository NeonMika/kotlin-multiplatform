import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

plugins {
    kotlin("multiplatform") version "2.0.20"
    // id("at.neon.k-perf-measure-plugin") version "0.0.1" // dependency on the k-perf-measure-plugin plugin
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal() // needed in addition to the pluginManagement block in settings.gradle.kts because the plugin in turn depends on another maven project
}

kotlin {
    jvm {

        compilations.all { }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        mainRun {
            // Define the main class to execute
            mainClass.set("JVMGameOfLifeApplicationKt")
        }
        compilations.all {
            tasks.withType<Jar> {
                doFirst {
                    manifest {
                        attributes(
                            "Main-Class" to "JVMGameOfLifeApplicationKt",
                            "Class-Path" to runtimeDependencyFiles.files.joinToString(" ") { it.name })
                    }
                }
                doLast {
                    copy {
                        from("build/libs")
                        from(runtimeDependencyFiles.files)
                        into("build/lib")
                    }
                }
            }
        }
    }
    js(IR) {
        /*
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        */
        nodejs()
        binaries.executable()
    }
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")

    /* https://kotlinlang.org/docs/multiplatform-dsl-reference.html#targets:
    A target that is not supported by the current host is ignored during building and, therefore, not published.
     */
    // val macosArm64 = macosArm64()
    // val macosX64 = macosX64()
    // val linuxArm64 = linuxArm64()
    // val linuxX64 = linuxX64()
    val mingwX64 = mingwX64()

    listOf(
        // macosArm64,
        // macosX64,
        // linuxArm64,
        // linuxX64,
        mingwX64
    ).forEach { target ->
        target.compilerOptions {
            verbose = true
            // freeCompilerArgs.add("-Xsave-llvm-ir-after=Codegen")
            // freeCompilerArgs.add("-Xsave-llvm-ir-directory=llvm-ir")
        }

        target.binaries {
            executable {
                entryPoint = "main"
                compilerOptions {
                    verbose = true
                    // freeCompilerArgs.add("-Xsave-llvm-ir-after=Codegen")
                    // freeCompilerArgs.add("-Xsave-llvm-ir-directory=llvm-ir")
                }
            }
            /*
            sharedLib {
                compilerOptions {
                    verbose = true
                    // freeCompilerArgs.add("-Xsave-llvm-ir-after=Codegen")
                    // freeCompilerArgs.add("-Xsave-llvm-ir-directory=llvm-ir")
                }
            }
            staticLib {
                compilerOptions {
                    verbose = true
                    // freeCompilerArgs.add("-Xsave-llvm-ir-after=Codegen")
                    // freeCompilerArgs.add("-Xsave-llvm-ir-directory=llvm-ir")
                }
            }
            if (hostOs == "Mac OS X") {
                framework {
                    compilerOptions {
                        verbose = true
                        // freeCompilerArgs.add("-Xsave-llvm-ir-after=Codegen")
                        // freeCompilerArgs.add("-Xsave-llvm-ir-directory=llvm-ir")
                    }
                }
            }
            */
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Because ktor client is using suspend functions
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
                // To perform network requests
                implementation("io.ktor:ktor-client-core:2.3.12")
                // To parse HTML
                implementation("com.fleeksoft.ksoup:ksoup:0.1.2")
                // To be able to create files in the compiler plugin
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.12")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.12")
            }
        }
        val jsTest by getting

        val mingwX64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:2.3.12")
            }
        }
        val mingwX64Test by getting
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        verbose = true
        freeCompilerArgs += listOf("-Xdump-fir", "-Xdump-ir")
    }
}