This subproject is intentionally excluded from settings.gradle since the 2026 dependency upgrade.

Reason:
  The old `de.richsource.gradle.plugins:gwt-gradle-plugin:0.4` (used by the
  original build) is incompatible with Gradle 7/8 and has been unmaintained
  since 2015. Its DSL (jetty plugin, `<<` task shorthand, `gwt { }` block)
  is fundamentally different from the modern gwt-gradle-plugin.

How to re-enable HTML/GWT support:
  1. Add the modern GWT Gradle plugin to settings.gradle pluginManagement:
         id 'org.gwtproject.gradle.gwt' version '1.1.0'
  2. Use GWT 2.10.0 (or newer) - update gwt {} block accordingly.
  3. Update html/src/com/ntk/darkmoor/GdxDefinition*.gwt.xml DOCTYPE to:
         <!DOCTYPE module PUBLIC "-//Google Inc.//DTD Google Web Toolkit 2.10.0//EN"
           "http://www.gwtproject.org/doctype/2.10.0/gwt-module.dtd">
  4. Replace the deprecated `jetty` plugin with the modern `org.gretty` plugin
     (or run the compiled output via any static web server).
  5. Add 'html' back to settings.gradle's include list.
  6. Rewrite html/build.gradle using the new plugin's DSL.

Reference: https://github.com/libgdx/libgdx/wiki/HTML5,-GWT-and-GWT-Gradle-plugin
