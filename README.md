# [WIP] Sealant

[![GitHub release](https://img.shields.io/maven-central/v/io.github.steinerok.sealant/di-common)](https://search.maven.org/search?q=g:io.github.steinerok.sealant)
[![License](https://img.shields.io/badge/license-apache2.0-blue?style=flat-square.svg)](https://opensource.org/licenses/Apache-2.0)

Sealant creates [Dagger] bindings and integrations for Android classes using the [Anvil-KSP].
This is meant to be an alternative to [Hilt], for those who'd prefer to enjoy the faster
compilation and better flexibility of Anvil-KSP.

Since Sealant is an extension upon Anvil-KSP, its code generation will be applied to **Kotlin** files
only.

Inspired by: Marcello Galhardo article [N26 Path to Anvil], Zac Sweers
article [Extending Anvil for Fun and Profit] and Rick Busarow library [Tangle]

## Setup

declare dependencies in `libs.versions.toml`:

```toml
[versions]
ksp = "2.3.7"
sealant = "0.6.0-alpha16"

[libraries]
sealant-diCommon = { module = "io.github.steinerok.sealant:di-common", version.ref = "sealant" }
sealant-core-runtime = { module = "io.github.steinerok.sealant:sealant-core-runtime", version.ref = "sealant" }
sealant-core-compiler-ksp = { module = "io.github.steinerok.sealant:sealant-core-compiler-ksp", version.ref = "sealant" }
sealant-appcomponent-runtime = { module = "io.github.steinerok.sealant:sealant-appcomponent-runtime", version.ref = "sealant" }
sealant-appcomponent-compiler-ksp = { module = "io.github.steinerok.sealant:sealant-appcomponent-compiler-ksp", version.ref = "sealant" }
sealant-fragment-runtime = { module = "io.github.steinerok.sealant:sealant-fragment-runtime", version.ref = "sealant" }
sealant-fragment-compiler-ksp = { module = "io.github.steinerok.sealant:sealant-fragment-compiler-ksp", version.ref = "sealant" }
sealant-viewmodel-runtime = { module = "io.github.steinerok.sealant:sealant-viewmodel-runtime", version.ref = "sealant" }
sealant-viewmodel-compiler-ksp = { module = "io.github.steinerok.sealant:sealant-viewmodel-compiler-ksp", version.ref = "sealant" }
sealant-work-runtime = { module = "io.github.steinerok.sealant:sealant-work-runtime", version.ref = "sealant" }
sealant-work-compiler-ksp = { module = "io.github.steinerok.sealant:sealant-work-compiler-ksp", version.ref = "sealant" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Add dependencies:

```gradle
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    // Common
    implementation(libs.sealant.diCommon)
    // Core
    implementation(libs.sealant.core.runtime)
    ksp (libs.sealant.core.compiler.ksp)
    // Appcomponent
    implementation (libs.sealant.appcomponent.runtime)
    ksp(libs.sealant.appcomponent.compiler.ksp)
    // Fragment
    implementation (libs.sealant.fragment.runtime)
    ksp(libs.sealant.fragment.compiler.ksp)
    // ViewModel
    implementation (libs.sealant.viewmodel.runtime)
    ksp(libs.sealant.viewmodel.compiler.ksp)
    // WorkManager
    implementation (libs.sealant.work.runtime)
    ksp(libs.sealant.work.compiler.ksp)
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
    addAppComponentSupport = true,
    addViewModelSupport = true,
    addFragmentSupport = true,
    addWorkSupport = true,
)
abstract class AppScope private constructor()
```

```kotlin
@SealantIntegration(scopes = [AppScope::class])
class SealantSampleApp : Application()
```

### AppComponent

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
) : Fragment()
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
class MainWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    /* Your dependencies */
) : CoroutineWorker(appContext, params)
```

## Contributions

Please contribute! I will gladly review any pull requests.
Make sure to read the [Contributing](CONTRIBUTING.md) page first though.

## License

    Copyright (c) 2022-2026 Ihor Kushnirenko

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Anvil-KSP]: https://github.com/zacsweers/anvil

[Dagger]: https://dagger.dev

[Hilt]: https://dagger.dev/hilt/

[N26 Path to Anvil]: https://dev.to/marcellogalhardo/n26-path-to-anvil-abd

[Extending Anvil for Fun and Profit]: https://dev.to/marcellogalhardo/n26-path-to-anvil-abd

[Tangle]: https://rbusarow.github.io/Tangle/
