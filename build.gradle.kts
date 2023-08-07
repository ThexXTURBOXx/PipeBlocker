plugins {
  id("java-library")
  id("maven-publish")
}

allprojects {
  apply(plugin = "java-library")
  // Project properties
  group = "info.mmpa.pipeblocker"
  version = "1.2.0"

  // Set the toolchain version to decouple the Java we run Gradle with from the Java used to compile and run the mod
  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(7))
      // Azul covers the most platforms for Java 7 toolchains, crucially including MacOS arm64
      vendor.set(JvmVendorSpec.AZUL)
    }
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
  }

  val embed: Configuration by configurations.creating {
    description = "Included in output JAR"
  }

  listOf(configurations.implementation).forEach {
    it.configure {
      extendsFrom(embed)
    }
  }

  tasks.named<Jar>("jar") {
    dependsOn(configurations["embed"])
    from(provider { configurations["embed"].map { if (it.isDirectory) it else zipTree(it) } })
  }
}