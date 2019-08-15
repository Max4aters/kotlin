buildscript {
    repositories {
        mavenCentral()
        maven {
            url('https://dl.bintray.com/jetbrains/intellij-plugin-service')
        }
    }
    dependencies {
        classpath 'org.jetbrains.intellij:plugin-repository-rest-client:0.4.32'
    }
}

task uploadPlugins {
    doLast {
        def env = System.getenv()
        def channel = env['PLUGIN_REPOSITORY_CHANNEL']
        if (channel == "_default_") {
            channel = null
        }
        def path = env['PLUGIN_UPLOAD_PATH']
        if (path == null) {
            path = "."
        }

        def token = project.property("plugins.repository.token")

        def repo = new org.jetbrains.intellij.pluginRepository.PluginRepositoryInstance("https://plugins.jetbrains.com/", token)

        File[] files = new File(path).listFiles({ _, String filename ->
            if (!filename.startsWith("kotlin-plugin") || !filename.endsWith(".zip")) false
            else {
                // don't publish CIDR plugins to IDEA channel
                def filenameLowerCase = filename.toLowerCase()
                if (filenameLowerCase.contains("clion") || filenameLowerCase.contains("appcode")) false
                else true
            }
        } as FilenameFilter)

        files = files.sort { f1, f2 ->
            f1.name.contains("1.1.2-5") ? 1 : (f2.name.contains("1.1.2-5") ? -1 : (f1.name <=> f2.name))
        }

        files.each { file ->
            println("Uploading ${file.name}")
            repo.uploadPlugin(6954, file, channel)
        }
    }
}
