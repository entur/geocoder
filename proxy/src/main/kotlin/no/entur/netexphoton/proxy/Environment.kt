package no.entur.netexphoton.proxy

import java.io.File

enum class Environment {
    DOCKER,
    KUBERNETES,
    CONSOLE,
    ;

    companion object {
        fun detect(): Environment =
            when {
                System.getenv("KUBERNETES_SERVICE_HOST") != null -> KUBERNETES
                isDocker() -> DOCKER
                else -> CONSOLE
            }

        private fun isDocker(): Boolean =
            File("/.dockerenv").exists() ||
                System.getenv("DOCKER_HOST") != null ||
                File("/proc/1/cgroup").run { exists() && readText().contains("container") }
    }
}
