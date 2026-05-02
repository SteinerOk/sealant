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

## What Sealant Generates

Sealant builds on top of [Anvil-KSP] and generates:

* member injectors for `Activity`, `Service`, `BroadcastReceiver` and `ContentProvider`
  classes annotated with `@InjectWith`
* `FragmentFactory` bindings for `@ContributesFragment`
* `ViewModelProvider.Factory` integration for `@ContributesViewModel`, including
  `SavedStateHandle`, `ViewModelLifecycle` and assisted creation callbacks
* `WorkerFactory` bindings for `@ContributesWorker`

Sealant code generation runs on **Kotlin** sources only.

## Setup

### Requirements

`0.6.0-beta03` is built against:

* Kotlin `2.3.21`
* KSP `2.3.7`
* Anvil-KSP `0.5.3`

Make sure `mavenCentral()` is available in your repositories:

```kotlin
repositories {
    mavenCentral()
}
```

### Version Catalog

```toml
[versions]
kotlin = "2.3.21"
ksp = "2.3.7"
anvilKsp = "0.5.3"
metro = "1.0.0"
sealant = "0.6.0-beta02"

[libraries]
anvilKsp-annotations = { module = "dev.zacsweers.anvil:annotations", version.ref = "anvilKsp" }
anvilKsp-annotations-optional = { module = "dev.zacsweers.anvil:annotations-optional", version.ref = "anvilKsp" }

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
anvilKsp = { id = "dev.zacsweers.anvil", version.ref = "anvilKsp" }
metro = { id = "dev.zacsweers.metro", version.ref = "metro" }
```

### 1. Configure the scope module

Add `sealant-core-runtime` and `sealant-core-compiler-ksp` to the module that declares your
Sealant scopes and application integration.

```kotlin
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.anvilKsp)
}

anvil {
    useKsp(
        contributesAndFactoryGeneration = true,
        componentMerging = true,
    )
    kspContributingAnnotations.addAll(
        "dev.steinerok.sealant.core.SealantConfiguration",
        "dev.steinerok.sealant.core.SealantIntegration",
    )
}

dependencies {
    implementation(libs.anvilKsp.annotations)
    implementation(libs.anvilKsp.annotations.optional)

    implementation(libs.sealant.diCommon)
    implementation(libs.sealant.core.runtime)
    ksp(libs.sealant.core.compiler.ksp)
}
```

### 2. Configure Android modules that use Sealant features

Add the runtime and compiler artifacts for the features you use. The example below enables
AppComponent, Fragment, ViewModel and WorkManager support in one Android module.

```kotlin
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.anvilKsp)
}

anvil {
    useKsp(
        contributesAndFactoryGeneration = true,
        componentMerging = true,
    )
    kspContributingAnnotations.addAll(
        "dev.steinerok.sealant.appcomponent.InjectWith",
        "dev.steinerok.sealant.fragment.ContributesFragment",
        "dev.steinerok.sealant.viewmodel.ContributesViewModel",
        "dev.steinerok.sealant.viewmodel.ContributesToViewModel",
        "dev.steinerok.sealant.work.ContributesWorker",
    )
}

dependencies {
    implementation(libs.anvilKsp.annotations)
    implementation(libs.anvilKsp.annotations.optional)

    implementation(libs.sealant.diCommon)
    implementation(libs.sealant.appcomponent.runtime)
    implementation(libs.sealant.fragment.runtime)
    implementation(libs.sealant.viewmodel.runtime)
    implementation(libs.sealant.work.runtime)

    ksp(libs.sealant.appcomponent.compiler.ksp)
    ksp(libs.sealant.fragment.compiler.ksp)
    ksp(libs.sealant.viewmodel.compiler.ksp)
    ksp(libs.sealant.work.compiler.ksp)
}
```

You only need to add the runtime/compiler pairs you actually use.

### Optional: Metro-backed generation

Sealant can also generate Metro-compatible bindings. Enable the Metro plugin and set the
`sealant.codegen.mode` KSP argument:

```kotlin
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
}

metro {
    enableKClassToClassMapKeyInterop = true
    interop {
        includeDagger(includeJakarta = false)
        includeAnvilForDagger(includeJakarta = false)
    }
}

ksp {
    arg("sealant.codegen.mode", "metroInterop")
}
```

## Usage

### 1. Declare scopes

Mark every scope that should get Sealant integrations with `@SealantConfiguration`.
Use `parentScope` when the scope is a child graph.

```kotlin
abstract class SkeletonScope private constructor()

@SealantConfiguration(
    addAppComponentSupport = true,
    addViewModelSupport = true,
    addFragmentSupport = true,
    addWorkSupport = true,
    parentScope = SkeletonScope::class,
)
abstract class AppScope private constructor()

@SealantConfiguration(
    addAppComponentSupport = true,
    addViewModelSupport = true,
    addFragmentSupport = true,
    addWorkSupport = false,
    parentScope = AppScope::class,
)
abstract class GuestScope private constructor()
```

### 2. Bootstrap the application

Annotate your `Application` with `@SealantIntegration` for every scope that should expose
Sealant-generated bindings. If you want easy access to the app graph, implement
`AppComponentProvider<T>`.

```kotlin
@SealantIntegration(scopes = [AppScope::class, GuestScope::class])
class SealantSampleApp : Application(), AppComponentProvider<AppComponent> {

    private val skeletonComponent: SkeletonComponent by lazy(LazyThreadSafetyMode.NONE) {
        SkeletonComponent.create(this)
    }
    
    override val appComponent: AppComponent by lazy(LazyThreadSafetyMode.NONE) {
        skeletonComponent.cast<AppComponent.Parent>().appComponent()
    }
}
```

### 3. Inject Android components

Use `@InjectWith` on Android classes that need member injection. Sealant generates injector maps
for `Activity`, `Service`, `BroadcastReceiver` and `ContentProvider`.

```kotlin
@InjectWith(AppScope::class)
class MainActivity : BaseActivity() {

    @Inject
    lateinit var appLevelTool: AppLevelTool

    override fun onCreate(savedInstanceState: Bundle?) {
        LocalAppInjection.inject(this)
        super.onCreate(savedInstanceState)
    }
}
```

One simple pattern is to centralize the actual injection call with `injectViaSealant(...)`:

```kotlin
object LocalAppInjection {

    fun inject(injectable: SealantInjectable<*>, appComponent: AppComponent) {
        injectViaSealant(injectable) { scope, _ ->
            when (scope) {
                AppScope::class -> appComponent
                else -> error("Unknown scope: $scope")
            }
        }
    }
}
```

### 4. Inject Fragments through `FragmentFactory`

Annotate constructor-injected fragments with `@ContributesFragment` and supply the generated
`SealantFragmentFactory` to the `FragmentManager`.

```kotlin
@ContributesFragment(scope = AppScope::class)
class MainFragment @Inject constructor(
    private val vmfCreator: SealantViewModelFactoryCreator,
    private val appLevelTool: AppLevelTool,
) : Fragment()
```

```kotlin
supportFragmentManager.fragmentFactory = appComponent
    .cast<SealantFragmentFactory.Owner>()
    .sealantFragmentFactory()
```

### 5. Create ViewModels

Annotate your `ViewModel` with `@ContributesViewModel`. `Application` and `SavedStateHandle`
can be injected directly, and `@ContributesToViewModel` lets you add scope-specific bindings to
the ViewModel graph.

```kotlin
@Module
@ContributesToViewModel(AppScope::class)
object AppScopeVmModule {

    @Provides
    fun provideInfo(handle: SavedStateHandle): Info =
        Info(handle.get<String>("time_1")!!)
}

@ContributesViewModel(AppScope::class)
class MainViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val info: Info,
) : ViewModel()
```

Use `@SingleIn(SealantViewModelScope::class)` inside `@ContributesToViewModel` modules when a
binding should live for exactly one generated ViewModel subcomponent.

To make Sealant-generated `ViewModel`s the default for an activity or fragment, delegate the
default factory to `SealantViewModelFactoryCreator`:

```kotlin
abstract class BaseFragment(
    private val vmfCreator: SealantViewModelFactoryCreator,
) : Fragment() {

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = vmfCreator.fromFragment(this, super.defaultViewModelProviderFactory)
}
```

Sealant also supports assisted ViewModels. Pass the assisted factory in
`@ContributesViewModel(..., assistedFactory = ...)` and provide the creation callback through
`withCreationCallback(...)`.

```kotlin
@ContributesViewModel(
    scope = AppScope::class,
    assistedFactory = MainViewModel.Factory::class,
)
class MainViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    @Assisted private val id: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): MainViewModel
    }
}
```

```kotlin
private val viewModel: MainViewModel by viewModels(
    extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<MainViewModel.Factory> { factory ->
            factory.create(requireArguments().getString("id")!!)
        }
    },
)
```

If you need lifecycle callbacks inside the ViewModel graph, inject `ViewModelLifecycle`.

### 6. Create WorkManager workers

Annotate workers with `@ContributesWorker`. Sealant generates bindings for
`SealantWorkerFactory`, which you can expose through `WorkManager` configuration.

```kotlin
@ContributesWorker(AppScope::class)
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SyncRepository,
) : CoroutineWorker(appContext, params)
```

```kotlin
class SealantSampleApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: SealantWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

## Migration Notes

Starting with `0.6.x`, Sealant publishes:

* `*-runtime` artifacts instead of the old `*-api` artifacts
* `*-compiler-ksp` artifacts instead of the old `*-codegen` and embedded compiler modules

If you are upgrading from an older version, update artifact names and make sure the relevant
modules are registered in `anvil.kspContributingAnnotations`.

## Samples

See the sample projects in this repository:

* `sample/anvil` for the standard Anvil-KSP setup
* `sample/metro` and `sample/metro-experiment` for Metro-backed generation

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
