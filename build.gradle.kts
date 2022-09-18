import org.gradle.internal.credentials.DefaultPasswordCredentials

plugins {
    id("java")
    id("net.researchgate.release") version "3.0.2"
    id("maven-publish")
    id("signing")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup:javapoet:1.13.0")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    testImplementation("com.google.testing.compile:compile-testing:0.19")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("java") {
            artifactId = "android-preference-annotations"
            from(components["java"])

            pom {
                name.set("android-preference-annotations")
                description.set("An annotation processor for type-safe access to shared preferences")
                url.set("https://github.com/jbb01/android-preference-annotations")

                developers {
                    developer {
                        id.set("jbb01")
                        name.set("Jonah Bauer")
                        email.set("publishing@jonahbauer.eu")
                        organizationUrl.set("https://github.com/jbb01")
                    }
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/jbb01/android-preference-annotations.git")
                    developerConnection.set("scm:git:https://github.com/jbb01/android-preference-annotations.git")
                    url.set("https://github.com/jbb01/anroid-preference/annotations")
                }
            }
        }
    }

    repositories {
        val credentials = DefaultPasswordCredentials().apply {
            username = project.findProperty("publishing.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("publishing.key") as String? ?: System.getenv("TOKEN")
        }

        maven {
            name = "central"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = credentials.username
                password = credentials.password
            }
        }

        maven {
            name = "snapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = credentials.username
                password = credentials.password
            }
        }
    }
}

signing {
    val key = project.findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    val password = project.findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
    useInMemoryPgpKeys(key, password)
    sign(publishing.publications["java"])
}

tasks {
    test {
        useJUnitPlatform()
    }

    javadoc {
        val options = (options as StandardJavadocDocletOptions)
        options.apply {
            tags("apiNote:a:API Note:")
            tags("implSpec:a:Implementation Requirements:")
            tags("implNote:a:Implementation Note:")
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    afterReleaseBuild {
        dependsOn(project.tasks["publishJavaPublicationToCentralRepository"])
    }
}