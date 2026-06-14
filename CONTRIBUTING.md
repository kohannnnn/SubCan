# Development checks

Format Kotlin and Gradle Kotlin DSL files before committing:

```shell
./gradlew spotlessApply
```

Run the same checks used by CI:

```shell
./gradlew spotlessCheck lintDebug testDebugUnitTest
```

Android Lint reports are generated under `app/build/reports/`.
