# 0.4.0

* fix tests when JPLOOT\_HOME is not set
* use OpenJ9 JVM for installer

# 0.3.1

* config: create parent directory before saving config

# 0.3.0

* install: add launcher generation

# 0.2.0

* add repository commands (add, remove, list)
* update jploot-maven-plugin to version 0.2 (m2e configuration)
* replaced bintray repository by nexus.tools.kobalt.fr
* update jploot-pom (${project.version} problem on some dependencyManagement)

# 0.1.2

* from jploot-pom 0.1.2: update repository configuration
  (nexus.tools.kobalt.fr)

# 0.1.1

* fix install command:

  * dependencies were not downloaded
  * application was not installed in jploot repository
  * packages were stored with an invalid path

# 0.1 (2020-12-28)

Basic features:

* installer:

  * self-extractable archive
  * stripped down JRE

* repository:

  * specific jploot local repository
  * remote repository list in config file

* common cli:

  * verbose/quiet output, config selection

* install command:

  * install from a maven gav a jar with a
    META-INF/jploot/jploot.properties descriptor

* remove command:

  * remove an installed application (by name or gav)

* list command:

  * list installed applications

* run command:

  * run from an application name
