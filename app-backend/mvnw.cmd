@ECHO OFF
SETLOCAL

SET WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
SET WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Downloading Maven Wrapper jar...
  SET MAVEN_WRAPPER_VERSION=3.3.2
  SET WRAPPER_JAR_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/%MAVEN_WRAPPER_VERSION%/maven-wrapper-%MAVEN_WRAPPER_VERSION%.jar
  curl -fSL -o "%WRAPPER_JAR%" "%WRAPPER_JAR_URL%"
)

SET JAVA_EXEC=java

"%JAVA_EXEC%" ^
  -cp "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=." ^
  "-Dmaven.wrapper.properties=%WRAPPER_PROPERTIES%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

ENDLOCAL

