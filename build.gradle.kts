/*
 * Copyright 2020 Uttran Ishtalingam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.4.21"
    application
    kotlin("plugin.serialization") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.google.cloud.tools.appengine-appyaml") version "2.4.1"
}

group = "com.dreamuth"
version = "1.0-SNAPSHOT"

val kotlinVersion = "1.4.21"
val serializationVersion = "1.0.1"
val ktorVersion = "1.4.3"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
    }
    js(LEGACY) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("io.ktor:ktor-websockets:$ktorVersion")
                implementation("io.ktor:ktor-html-builder:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

                // Google secrets
                implementation("com.google.cloud:google-cloud-secretmanager:1.2.6")

                // Email
                implementation("com.sun.mail:javax.mail:1.6.2")

                // Sheets report
                implementation("com.google.auth:google-auth-library-oauth2-http:0.22.2")
                implementation("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
                implementation("io.ktor:ktor-client-websockets:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")

                implementation(npm("react", "17.0.1"))
                implementation(npm("react-dom", "17.0.1"))
                implementation(npm("react-is", "17.0.1"))
                implementation("org.jetbrains:kotlin-react:17.0.0-pre.132-kotlin-$kotlinVersion")
                implementation("org.jetbrains:kotlin-react-dom:17.0.0-pre.132-kotlin-$kotlinVersion")
                implementation("org.jetbrains:kotlin-styled:5.2.0-pre.132-kotlin-$kotlinVersion")
            }
        }
        val jsTest by getting
    }
}

application {
    mainClassName = "com.dreamuth.ServerKt"
//    mainClass.set("com.dreamuth.ServerKt")
}

tasks.getByName<Jar>("jvmJar") {
    val taskName = if (project.hasProperty("isProduction")) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val webpackTask = tasks.getByName<KotlinWebpack>(taskName)
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) // bring output file along into the JAR
}

tasks.getByName<Jar>("shadowJar") {
    val taskName = if (project.hasProperty("isProduction")) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val webpackTask = tasks.getByName<KotlinWebpack>(taskName)
    dependsOn(webpackTask)
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName))
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName + ".map"))
}

tasks.getByName("installShadowDist") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
}

tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}

// Alias "installDist" as "stage" (for cloud providers)
tasks.create("stage") {
    dependsOn(tasks.getByName("installDist"))
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

appengine {
    stage.setArtifact("$buildDir/libs/${project.name}-${project.version}-all.jar")
    deploy {
        projectId = "thirukkural-games"
        version = "v2"
        stopPreviousVersion = true
        promote = true
    }
}

configure<com.google.cloud.tools.gradle.appengine.appyaml.AppEngineAppYamlExtension> {
    stage.setAppEngineDirectory("src/jvmMain/appengine")
}
