#! /bin/bash

JAVA_HOME=/usr/lib/jvm/java-11
MVN=mvn-3.6
VERSION=1.0-SNAPSHOT
export JAVA_HOME

which asciidoctor >/dev/null || { echo asciidoctor is missing>2; exit 1; }

JARS="$PWD/manpages/jploot-jars"
ADOC="$PWD/manpages/adoc"
MAN="$PWD/manpages/man"

mkdir -p "$ADOC"
mkdir -p "$MAN"
rm -rf "$JARS"
mkdir -p "$JARS"

"$MVN" clean install -DskipTests -am -pl :jploot-cli
"$MVN" dependency:copy -Dartifact=jploot:jploot-cli:1.0-SNAPSHOT:jar "-DoutputDirectory=$JARS"
"$MVN" dependency:copy-dependencies -Dartifact=jploot:jploot-cli:1.0-SNAPSHOT:jar "-DoutputDirectory=$JARS"
"$MVN" dependency:copy -Dartifact=info.picocli:picocli-codegen:4.5.2:jar "-DoutputDirectory=$JARS"

"$JAVA_HOME/bin/java" -Duser.language=en -cp "$JARS/*" picocli.codegen.docgen.manpage.ManPageGenerator -d "$ADOC" jploot.cli.JplootMain
asciidoctor --backend=manpage "--source-dir=$ADOC" "--destination-dir=$MAN" *.adoc
#"$JAVA_HOME/bin/java" -Duser.language=en -cp $( mvn-3.6 dependency:build-classpath -q -DincludeTypes=jar -Dmdep.includeScope=runtime -Dmdep.outputFile=/dev/stdout -pl :jploot-cli ):/home/lalmeras/.m2/repository/info/picocli/picocli-codegen/4.5.2/picocli-codegen-4.5.2.jar:/home/lalmeras/.m2/repository/jploot/jploot-cli/1.0-SNAPSHOT/jploot-cli-1.0-SNAPSHOT.jar picocli.codegen.docgen.manpage.ManPageGenerator -vvv jploot.cli.JplootMain
