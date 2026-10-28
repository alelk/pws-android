import com.android.build.api.dsl.ApplicationExtension

// Android application module of pws-android: same common base as `pws.android.library`.
// applicationId, flavors, signing and build types stay in the application module.

plugins {
  id("com.android.application")
  id("pws.static-analysis")
}

val android = extensions.getByType<ApplicationExtension>()
configureAndroidCommon(android)
android.defaultConfig.targetSdk = ANDROID_COMPILE_SDK
