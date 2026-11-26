# The Legend Of Darkmoor
Java remake of the classic RPG **Eye Of The Beholder II: The Legend of Darkmoon** for Android and PC platforms.

The project is based on [libGDX](https://libgdx.com/) and is currently under development.
It consists mainly of an engine to generate dungeon crawlers in the style of the classic E.O.B.
The engine resource system is compatible with
[Dungeon Eye](http://www.dungeoneye.net/), which offers an editor for resources and levels that can be used to generate multi-platform games with the darkmoor engine.

## Build status (2026 dependency upgrade)

| Toolchain / dependency | Old (2015)         | New (2026)         |
|------------------------|--------------------|--------------------|
| Gradle wrapper         | 2.2                | **8.7**            |
| Android Gradle Plugin  | 1.0.0              | **8.5.2**          |
| libGDX                 | 1.5.3 (LWJGL2)     | **1.14.2 (LWJGL3)**|
| Java source level      | 1.6                | **1.8 core / 21 desktop** |
| Java toolchain (build) | 1.6/1.7            | **JDK 21**         |
| Android minSdk         | 8                  | **21**             |
| Android targetSdk      | 19                 | **34**             |
| RoboVM (iOS)           | 1.0.0-beta-03      | **removed** (dead) |
| GWT (HTML)             | 0.4 + GWT 2.6.0    | **removed** (legacy)|

### Active subprojects
- `core`     - shared game code (Java 8 bytecode so Android can consume it)
- `desktop`  - LWJGL3 backend launcher (Java 21)
- `android`  - Android backend launcher

### Disabled subprojects (source kept for reference)
- `ios`   - RoboVM is unmaintained. To re-enable, use [MobiVM](https://github.com/MobiVM/robovm) (a maintained RoboVM fork) and the `gdx-backend-robovm` artifact.
- `html`  - the old `de.richsource.gradle.plugins:gwt-gradle-plugin:0.4` is incompatible with Gradle 8. To re-enable, switch to `org.gwtproject.gradle:gwt-gradle-plugin:1.1.0` + GWT 2.10+ and rewrite `html/build.gradle` accordingly.

### How to build
Prerequisites:
- JDK 21 installed (`java -version` shows 21.x)
- Android SDK with platform 34 + build-tools 34.x (only needed for the android subproject)
- Internet access (first build downloads libGDX 1.14.2 + LWJGL3 natives)

Create `local.properties` at the project root (only for Android builds):
```
sdk.dir=/path/to/Android/Sdk
```

Run from the project root:
```bash
# Desktop (the easiest way to verify the upgrade compiles and runs)
./gradlew desktop:run

# Desktop distributable jar
./gradlew desktop:dist

# Android debug APK
./gradlew android:assembleDebug
```

### Known follow-up work
After applying this upgrade, the project compiles against libGDX 1.14.2 / LWJGL3, but the original game code was
written against libGDX 1.5.x and may still hide subtle runtime differences:
- `ScissorStack.calculateScissors` has been migrated to the 8-arg overload (see `MazeGroup.drawChildren`).
- `LwjglApplication` -> `Lwjgl3Application` (see `DesktopLauncher`); LWJGL3 has a different input/windowing model,
  so some `Gdx.input` behavior (cursor, multi-window, clipboard) may differ from LWJGL2.
- `FreeTypeFontGenerator` default character set is unchanged; verify font glyphs render correctly on first launch.
- `AssetManager` API is stable but `AssetErrorHandling` defaults changed in 1.10+ — errors now bubble up by default.

The current stage of implementation of the game covers:
* Loading a predefined saved game. That's for now the only way to enter the game, no new adventure party can be created
* setting some options, music, hit points as bar/numbers, etc..
* moving into the dungeon freely, forest, temple level 1,2 and dungeons. Enjoy while there are no enemies around! (hurry, arriving any moment now)
* swinging weapons...but in the air

more features coming up soon. contributors are welcome.

##Screenshots
![main game menu][1]

![temple of darkmoor][2]

![temple dungeon][3]

![forest][4]


[1]: https://github.com/ntk4/darkmoor/blob/master/doc/screenshots/mainmenu.png
[2]: https://github.com/ntk4/darkmoor/blob/master/doc/screenshots/temple.png
[3]: https://github.com/ntk4/darkmoor/blob/master/doc/screenshots/dungeon.png
[4]: https://github.com/ntk4/darkmoor/blob/master/doc/screenshots/forest.png
