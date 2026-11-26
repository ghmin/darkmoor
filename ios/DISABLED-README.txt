This subproject is intentionally excluded from settings.gradle since the 2026 dependency upgrade.

Reason:
  RoboVM (com.badlogicgames.gdx:gdx-backend-robovm) has been unmaintained
  since 2015 and does not work with libGDX 1.14.x, Gradle 8.x or JDK 21.

How to re-enable iOS support:
  1. Switch to the MobiVM fork of RoboVM (https://github.com/MobiVM/robovm)
     which is actively maintained.
  2. Add the MobiVM Gradle plugin to the root buildscript block:
         classpath 'com.mobidevelop.robovm:robovm-gradle-plugin:2.3.22'
  3. Replace the old backend dependency:
         implementation "com.mobidevelop.robovm:robovm-rt:${roboVMVersion}"
         implementation "com.mobidevelop.robovm:robovm-cocoatouch:${roboVMVersion}"
         implementation "com.badlogicgames.gdx:gdx-backend-robovm:$gdxVersion"
  4. Add 'ios' back to settings.gradle's include list.
  5. Rewrite ios/build.gradle using the modern robovm plugin DSL.
