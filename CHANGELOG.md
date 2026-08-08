Change Log
==========

## Version 0.7.0-beta02

_2026-08-08_

* Class-level `@Inject` and `@AssistedInject` are now supported: like Metro, a class-level
  annotation applies to the class's single primary constructor. Generated
  `<Type>_SealantInjector` classes now emit `@Inject` on the class instead of the constructor,
  and the ViewModel and Worker symbol processors accept either annotation placement. This is
  the recommended style for classes with exactly one constructor and silences Metro's
  "There is only one @Inject-annotated constructor" warning on generated code.
* Upgrade Gradle to `9.7.0`, Metro to `1.4.1`.

## Version 0.7.0-beta01

_2026-08-03_

* **Breaking:** Sealant is now Metro-only. The Dagger/Anvil generation path, `sample/anvil`,
  `sample/metro-experiment`, and the `sealant.codegen.mode=metroInterop` option have been removed.
  All generated bindings target `dev.zacsweers.metro.*` annotations, and the `metro` Gradle plugin
  is required in every module consuming Sealant-generated code.
* Runtime multibinding maps now use `KClass` keys (`Map<KClass<out T>, V>`) instead of `Class`
  keys, matching Metro's `@MapKey` semantics.
* Scoped child graphs are generated as Metro `@GraphExtension`s with contributed
  `@GraphExtension.Factory` types instead of Dagger subcomponents.

## Version 0.6.0-beta04

_2026-05-08_

* Refactor generated ViewModel integration to use the `<Scope>_ViewModel` naming convention
  consistently across symbol processors and generated components.
* Upgrade Anvil KSP to `0.5.4`, KotlinX Coroutines to `1.11.0`, and the Android Gradle Plugin
  to `9.2.1`.

## Version 0.6.0-beta03

_2026-05-02_

* Improve documentation across the runtime APIs, KSP utilities, symbol processors, and README to
  better explain the generated components and Sealant integration points.
* Refactor symbol processor validation to use a shared predicate for AST node filtering and
  improve KSP validation performance.

## Version 0.6.0-beta02

_2026-04-28_

* Migrate Sealant code generation to Anvil-KSP and publish new `*-runtime` and `*-compiler-ksp`
  artifacts for core, appcomponent, fragment, viewmodel, and work integrations. This release
  replaces the previous `*-api`, `*-codegen` and embedded compiler modules.
* Reorganize project modules and build logic, add a version catalog and convention plugins, and
  upgrade Gradle, Kotlin, KSP, AndroidX, and Dagger-related dependencies. This release requires
  Kotlin `2.3.21` and KSP `2.3.7`.
* Expand AppComponent integration to additional Android component types and add the
  `AppComponentProvider` interface.
* Add Metro support for Dagger integration via the `sealant.codegen.mode=metroInterop` option.
* Enhance ViewModel support with `SealantViewModelSupport`, assisted injection factories,
  improved generated multibindings and subcomponent bindings, and lifecycle-aware creation with
  `ViewModelLifecycle` and retained lifecycle support.
* Remove deprecated `AbstractSavedStateViewModelFactory` usage and legacy ViewModel factory
  wiring, and improve generated modules, interfaces, and documentation for fragments, workers, and
  viewmodels.

## Version 0.3.1

_2023-11-18_

* Upgrade Kotlin to `1.9.20` and KotlinPoet to `1.15.0`.

## Version 0.3.0

_2023-09-09_

* Upgrade Anvil to `2.4.8`.
* Use `@SingleIn` annotation from new `annotations-optional` artifact.

## Version 0.2.6

_2023-09-03_

* Upgrade Kotlin to `1.9.10`.
* Fix compatibility with Dagger `2.48`.

## Version 0.2.5

_2023-07-31_

* Upgrade Kotlin to `1.9.0`, Anvil to `2.4.7` and KotlinPoet to `1.14.2`. This release is not
  compatible with lower versions of Kotlin and Anvil.

## Version 0.2.4

_2023-05-26_

* Upgrade Kotlin to `1.8.21`, Anvil to `2.4.6` and KotlinPoet to `1.13.2`. This release is not
  compatible with lower versions of Kotlin and Anvil.

## Version 0.2.3

_2023-04-07_

* Revert KotlinPoet to `1.12.0` due to generation errors in WorkerAssistedFactory.
* Fix WorkerParameters variable name in Workers.

## Version 0.2.2

_2023-04-07_

* Upgrade Kotlin to `1.8.20` and Anvil to `2.4.5`. This release is not compatible with lower
  versions of Kotlin and Anvil.

## Version 0.2.1

_2023-02-13_

* Upgrade Kotlin to `1.8.10`.

## Version 0.2.0

_2023-01-13_

* Upgrade Kotlin to `1.8.0` and Anvil to `2.4.4`. This release is not compatible with lower
  versions of Kotlin and Anvil.

## Version 0.1.0

_2022-08-12_

* Initial public release.
