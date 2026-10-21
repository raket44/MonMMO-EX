package pirated.server.login.config

data class DbConfig(
    val host: String = "localhost",
    val port: Int = 20011,
    val name: String = "pirated_login_db",
    val user: String = "pirated_login_user",
    val password: String = "changeMe!",
    val poolSize: Int = 4,
    val seedDev: Boolean = false,
) {
  val jdbcUrl: String
    get() = "jdbc:postgresql://$host:$port/$name"
}
