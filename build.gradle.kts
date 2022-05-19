import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  `maven-publish`
}

group = "io.err0.logback"
group = "io.err0.logback"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      artifactId = "err0-logback"
      from(components["java"])
      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }
      pom {
        name.set("err0-logback")
        description.set("Logback adapter for err0")
      }
    }
  }
  repositories {
    maven {
      url = uri("https://gitlab.bluetrail.software/api/v4/projects/" + System.getenv("CI_PROJECT_ID") + "/-/packages/maven")
      name = "BTS-GitLab"
    }
  }
}

val junitJupiterVersion = "5.7.0"

val launcherClassName = "io.err0.logback.Main"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  //implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  //implementation("io.vertx:vertx-web-client")
  //testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")

  // https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5
  implementation("org.apache.httpcomponents.client5:httpclient5:5.1")
  implementation("com.google.code.gson:gson:2.8.7")

  // the below are uncommented to allow me to use autocomplete to edit the
  // java unit test cases, please comment these out as the tests don't actually
  // depend on these:
  // testImplementation("org.apache.logging.log4j:log4j-core:2.14.1")
  // testImplementation("org.slf4j:slf4j-api:1.7.31")

  // https://mvnrepository.com/artifact/ch.qos.logback/logback-core
  implementation("ch.qos.logback:logback-core:1.2.11")
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
  implementation("ch.qos.logback:logback-classic:1.2.11")

  // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
  implementation("org.slf4j:slf4j-api:1.7.36")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}