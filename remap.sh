cd ../yarn
./gradlew tinyJar
./gradlew publishToMavenLocal
cd ../acuity-fabric
./gradlew cleanLoomMappings
cp /Users/grondag/.m2/repository/net/fabricmc/yarn/18w50a.local/yarn-18w50a.local.jar /Users/grondag/.gradle/caches/fabric-loom/
./gradlew remapSources
./gradlew genSources