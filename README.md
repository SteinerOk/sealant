# [WIP] Sealant

[![GitHub release](https://img.shields.io/maven-central/v/io.github.steinerok.sealant/di-common)](https://search.maven.org/search?q=g:io.github.steinerok.sealant)
[![License](https://img.shields.io/badge/license-apache2.0-blue?style=flat-square.svg)](https://opensource.org/licenses/Apache-2.0)

Sealant creates [Dagger] bindings and integrations for Android classes using the [Anvil].
This is meant to be an alternative to [Hilt], for those who'd prefer to enjoy the faster
compilation and better flexibility of Anvil.

Since Sealant is an extension upon Anvil, its code generation will be applied to **Kotlin** files
only.

Inspired by: Marcello Galhardo article [N26 Path to Anvil], Zac Sweers
article [Extending Anvil for Fun and Profit] and Rick Busarow library [Tangle]

## Setup

Add dependencies:

```gradle
dependencies {
    def sealant_version = "0.2.5"

    // Common
    implementation "io.github.steinerok.sealant:di-common:${sealant_version}"
    // Core
    implementation "io.github.steinerok.sealant:sealant-core-api:${sealant_version}"
    anvil "io.github.steinerok.sealant:sealant-core-codegen:${sealant_version}"
    // Appcomponent
    implementation "io.github.steinerok.sealant:sealant-appcomponent-api:${sealant_version}"
    anvil "io.github.steinerok.sealant:sealant-appcomponent-codegen:${sealant_version}"
    // Fragment
    implementation "io.github.steinerok.sealant:sealant-fragment-api:${sealant_version}"
    anvil "io.github.steinerok.sealant:sealant-fragment-codegen:${sealant_version}"
    // ViewModel
    implementation "io.github.steinerok.sealant:sealant-viewmodel-api:${sealant_version}"
    anvil "io.github.steinerok.sealant:sealant-viewmodel-codegen:${sealant_version}"
    // WorkManager
    implementation "io.github.steinerok.sealant:sealant-work-api:${sealant_version}"
    anvil "io.github.steinerok.sealant:sealant-work-codegen:${sealant_version}"
}
```

Make sure that you have `mavenCentral()` in the list of repositories:

```gradle
repositories {
    mavenCentral()
}
```

## Features

### Core

```kotlin
@SealantConfiguration(
    addAppcomponentSupport = true,
    addViewModelSupport = true,
    addFragmentSupport = true,
    addWorkSupport = true,
)
abstract class AppScope private constructor()
```

### Appcomponent

```kotlin
@InjectWith(AppScope::class)
class MainActivity : ComponentActivity() {
    /* Your dependencies, inject via lateinit var and @Inject annotation */
}
```

### Fragment

```kotlin
@ContributesFragment(scope = AppScope::class)
class MainFragment @Inject constructor(
    /* Your dependencies */
)
```

### ViewModel

```kotlin
@ContributesViewModel(AppScope::class)
class MainViewModel @Inject constructor(
    private val application: Application,
    private val ssHandle: SavedStateHandle,
    /* Your dependencies */
) : ViewModel()
```

### Work

```kotlin
@ContributesWorker(AppScope::class)
class MainWorker2 @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    /* Your dependencies */
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = TODO()
}
```

## Contributions

Please contribute! I will gladly review any pull requests.
Make sure to read the [Contributing](CONTRIBUTING.md) page first though.

## License

    Copyright (c) 2022-2023 Ihor Kushnirenko

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Anvil]: https://github.com/square/anvil

[Dagger]: https://dagger.dev

[Hilt]: https://dagger.dev/hilt/

[N26 Path to Anvil]: https://dev.to/marcellogalhardo/n26-path-to-anvil-abd

[Extending Anvil for Fun and Profit]: https://dev.to/marcellogalhardo/n26-path-to-anvil-abd

[Tangle]: https://rbusarow.github.io/Tangle/
