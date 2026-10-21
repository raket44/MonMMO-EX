package buildsrc.common

val KEYS_RESOURCE_LOCATION = "src/main/resources"

tasks.register<Copy>("copyPublicKeyChat") {
  group = "pirated"
  description = "Copy chat keys from \":keys\" to the current projects resources directory"
  from(project(":keys").tasks.getByName("generateChat").outputs.files) {
    include("*.public.pem")
  }
  into(project.layout.projectDirectory.dir(KEYS_RESOURCE_LOCATION))
}

tasks.register<Copy>("copyPrivateKeyChat") {
  group = "pirated"
  description = "Copy chat keys from \":keys\" to the current projects resources directory"
  from(project(":keys").tasks.getByName("generateChat").outputs.files) {
    include("*.public.pem")
  }
  into(project.layout.projectDirectory.dir(KEYS_RESOURCE_LOCATION))
}

tasks.register("copyKeysChat") {
  group = "pirated"
  description = "Copies all chat keys from \":keys\" to the current projects resources directory"
  dependsOn("copyPublicKeyChat", "copyPrivateKeyChat")
}

tasks.register<Copy>("copyPublicKeyGame") {
  group = "pirated"
  description = "Copy game keys from \":keys\" to the current projects resources directory"
  from(project(":keys").tasks.getByName("generateGame").outputs.files) {
    include("*.public.pem")
  }
  into(project.layout.projectDirectory.dir(KEYS_RESOURCE_LOCATION))
}

tasks.register<Copy>("copyPrivateKeyGame") {
  group = "pirated"
  description = "Copy game keys from \":keys\" to the current projects resources directory"
  from(project(":keys").tasks.getByName("generateGame").outputs.files) {
    include("*.private.pem")
  }
  into(project.layout.projectDirectory.dir(KEYS_RESOURCE_LOCATION))
}

tasks.register("copyKeysGame") {
  group = "pirated"
  description = "Copies all game keys from \":keys\" to the current projects resources directory"
  dependsOn("copyPublicKeyGame", "copyPrivateKeyGame")
}

tasks.register<Copy>("copyPublicKeyFeed") {
  group = "pirated"
  description = "Copy feed keys from \":keys\" to the current projects resources directory"
  from(project(":keys").tasks.getByName("generateFeed").outputs.files) { include("*.public.pem") }
  into(project.layout.projectDirectory.dir(KEYS_RESOURCE_LOCATION))
}

tasks.register<Copy>("copyPrivateKeyFeed") {
  group = "pirated"
  description = "Copy feed keys from \":keys\" to the current projects resources directory"
  from(project(":keys").tasks.getByName("generateFeed").outputs.files) { include("*.private.pem") }
  into(project.layout.projectDirectory.dir(KEYS_RESOURCE_LOCATION))
}

tasks.register("copyKeysFeed") {
  group = "pirated"
  description = "Copies all feed keys from \":keys\" to the current projects resources directory"
  dependsOn("copyPublicKeyFeed", "copyPrivateKeyFeed")
}

tasks.register("copyPublicKeys") {
  group = "pirated"
  description = "Copies all public keys from \":keys\" to the current projects resources directory"
  dependsOn("copyPublicKeyChat", "copyPublicKeyGame", "copyPublicKeyFeed")
}

tasks.register("copyKeys") {
  group = "pirated"
  description = "Copies all keys from \":keys\" to the current projects resources directory"
  dependsOn("copyKeysChat", "copyKeysGame", "copyKeysFeed")
}

// Generated keys are for local runs. Published archives ship none.
tasks.withType<Jar>().configureEach { exclude("*.pem") }

val keyCopyTasks =
    listOf(
        "copyPublicKeyChat",
        "copyPrivateKeyChat",
        "copyPublicKeyGame",
        "copyPrivateKeyGame",
        "copyPublicKeyFeed",
        "copyPrivateKeyFeed",
    )

tasks.matching {
      it.name.startsWith("spotless") || it.name.startsWith("sonarlint") || it.name == "jooqCodegen"
    }
    .configureEach { mustRunAfter(keyCopyTasks) }