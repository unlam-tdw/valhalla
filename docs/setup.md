# Development Environment Setup

Before working with the project, install and configure these tools.

## Java

Java is the programming language used in this project. The project is configured to run with Java 25 (LTS).

1. Download the JDK for your operating system: [JDK 25 Temurin](https://adoptium.net/temurin/releases/?version=25).
2. Unzip the downloaded file into a folder.
   - Example: `/home/java` (Linux) or `C:\java` (Windows).
3. Set an environment variable named `JAVA_HOME` pointing to that folder.
   - Example: `C:\java\jdk-25` (Windows) or `/home/java/jdk-25` (Linux).
4. Add `JAVA_HOME` to the `PATH` environment variable:
   - Add `%JAVA_HOME%\bin` to the existing list.
5. Run `java -version` and `javac -version` in CMD or Terminal; you should see the installed Java version and the compiler version as output, respectively.

- [Guide to installing Java on Windows](https://www.java.com/es/download/help/windows_manual_download.html)
- [Guide to installing Java on Linux](https://www.java.com/es/download/help/linux_x64_install.html)

## Maven

Maven is a project management tool (mainly for Java projects). It simplifies and standardizes the software build process.

1. Java 25 must be installed and environment variables configured (`JAVA_HOME` and `PATH`).
2. Download Maven from the [official site](https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip) and unzip it into a folder.
3. Set an environment variable named `MAVEN_HOME` pointing to that folder.
   - Example: `/home/maven/apache-maven-3.9.16` (Linux) or `C:\maven\apache-maven-3.9.16` (Windows).
4. Add `MAVEN_HOME` to the `PATH` environment variable:
   - Add `%MAVEN_HOME%\bin` to the existing list.
5. Run `mvn -version` in CMD or Terminal; you should see the downloaded Maven version as output.
6. **.m2 folder**: Maven's local repository where it stores artifacts (like JAR files), downloaded as dependencies or generated locally. By default this folder is created in:
   - Linux: `/home/<user>/.m2`
   - Windows: `C:\Users\<user>\.m2`

- [Guide to installing Maven](https://maven.apache.org/install.html)

### IDE configuration

- **IntelliJ**: Maven comes installed and the plugin is available in the panel on the right (shown with the letter **M**). It lets you run commands and manage plugins and dependencies.
- **VS Code** (recommended plugins):
  - **Maven for Java**: the official plugin. From the explorer, a Maven section appears below the project.
  - **XML**: improves autocompletion and syntax validation in XML files. For Maven the `pom.xml` file is crucial.

## Docker

Docker is a container platform that packages applications together with all their dependencies into lightweight, portable containers.

- **Windows**: install `Docker Desktop`. Follow this [guide](https://docs.docker.com/desktop/setup/install/windows-install/).
- **Linux**: install `Docker Engine` by following this [guide](https://docs.docker.com/engine/install/ubuntu/). You can also install `Docker Desktop` (it includes Docker Engine).

## Environment Variables

The app connects to PostgreSQL via environment variables. Copy `.env.example` to `.env` and fill in the values:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `valhalla` |
| `DB_USER` | Database user | `user` |
| `DB_PASSWORD` | Database password | `user` |
| `POSTGRES_DB` | Initial DB created by the Postgres container | `valhalla` |
| `POSTGRES_USER` | Initial superuser created by the Postgres container | `user` |
| `POSTGRES_PASSWORD` | Initial superuser password | `user` |

`DB_*` variables are read by the Java app at runtime. `POSTGRES_*` variables are only used by the Postgres Docker container on first boot.

```shell
cp .env.example .env
# Edit .env with your values
```

**Tip:** the defaults work out of the box , you only need to edit `.env` if you want custom credentials.
