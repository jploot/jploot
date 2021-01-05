# Description

jploot is a java package manager aiming at managing local installation of java
programs. Primary goals of jploot are:

* managing java CLI programs (not primarily dedicated to servers apps,
  services, ...)
* managed by command line
* non privileged tool (installation are done in user folders)
* allow multi-version installs
* allow asset reuse: jar reuse between programs, local java installation reuse

# Build requirement

* jploot-installer build:
  * needs jploot.installer.javaHome property to be set with a OpenJ9 JVM path
    (current version uses -Xshareclasses specific option)
  * needs makeself command
