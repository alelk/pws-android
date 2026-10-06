import com.android.build.api.dsl.LibraryExtension

// Android library module of pws-android: SDK levels, Java 21 toolchain, unit-test options and the
// `localSeed` build type. Namespace, flavors, build types and dependencies stay in the module.

plugins {
  id("com.android.library")
}

configureAndroidCommon(extensions.getByType<LibraryExtension>())
