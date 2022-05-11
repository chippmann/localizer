# Localization Gradle Plugin
Gradle plugin for generating localization files for Android and small Kotlin Multiplatform projects

## Overview
This gradle plugin provides gradle tasks to generate localization files either from local `csv` files or from [Google Sheets](https://www.google.com/sheets/about/).

The plugin does nothing else than providing those tasks. Setting them up and triggering them is up to the project which includes this plugin.

Currently, only string resources are supported.


## Limitations
The plugins Multiplatform tasks are really only suited for small Kotlin Multiplatform projects as they generate a single global `object` holding all translations. Which means upon first usage of a translation, all translations are kept in memory for the whole lifetime of the application.

The aim these tasks for Kotlin Multiplatform projects at the moment is just ease of use for translations used in common code. There are plans however to improve this so this plugin can also be used for big Kotlin Multiplatform projects where one want's to use translations in common code.

**These limitations do not apply to the android specific tasks** where normal xml string resources are generated instead.


## Usage
- Add the plugin to your `plugins` block inside `build.gradle.kts`:
    ```kotlin
  plugins {
      id("ch.hippmann.localizer") version "0.0.1"
  }
    ```

- Configure the tasks you want to use:
    ```kotlin
  tasks {
      val generateTranslations by registering(GenerateAndroidStringLocalizationFromGoogleSheetsTask::class) {
        group = "myProject" // define a group so the task can be found more easily on the gradle tasks tab in the IDE
        sheetId.set("<sheet-id>") // provide the google sheets id (can be found in the url of the sheet)
        sheetIndex.set(0)
        srcRoot.set(projectDir.resolve("src"))
        throwIfDownloadFailed.set(true)
        baseLanguage.set("en")
      }
  
      build {
        // execute translations task on each build
        dependsOn(generateTranslations)
      }
  }
    ```

## Available Tasks
- `DownloadGoogleSheetsAsCsvTask`: Downloads a google sheet to a specified location
- `GenerateAndroidStringLocalizationFromCsvFileTask`: Generates xml string res files from a local csv file
- `GenerateAndroidStringLocalizationFromGoogleSheetsTask`: Generates xml string res files from google sheets
- `GenerateMultiplatformStringLocalizationFromCsvFileTask`: Generates a translation file `object` from a local csv file
- `GenerateMultiplatformStringLocalizationFromGoogleSheetsTask`: Generates a translation file `object` from google sheets


## Requirements
If you use a google sheets, it has to be shared to "Anyone with this link" with viewer permissions.

The following formatting rules must be met by the source `csv` file or google sheets:
- The first row is considered to be the header row.
- There must be the following columns present:
  - `key`
  - `platform`: valid values are: `both`, `android`, `ios`
  - each language as separate column (ex. `en`, `de`, `de-ch`, `fr`, `fr-ch`)
  - the language columns must come after the `platform` column

Example of a valid format:

| key      | platform | en          | de              |
|----------|----------|-------------|-----------------|
| app_name | both     | My cool App | Meine coole App |

**Note:** Between the columns `key` and `platform` you are free to define any additional columns you want. For example a `description` column. These additional columns will just be ignored.

## Development
To be able to test the plugin with a local build, one needs to execute `publishToMavenLocal` and add the following to
the top of the app's `settings.gradle.kts` file:

*Gradle KTS:*
 ```kotlin
 // without this doing:
 //  plugin { id("ch.hippmann.localizer") version "0.0.1-SNAPSHOT" }
 // won't work  as gradle does not know how to map the plugin id to an actual artifact.
 // this is only required when trying out local builds. Comment this out when trying out a plugin published
 // in the gradle plugin portal.
 pluginManagement {
     repositories {
         mavenLocal()
         jcenter()
         gradlePluginPortal()
     }
 
     resolutionStrategy.eachPlugin {
         when (requested.id.id) {
             "ch.hippmann.localizer" -> useModule("ch.hippmann.localizer:${requested.version}")
         }
     }
 }

```
*Gradle groovy:*
```groovy
 // without this doing:
 //  plugin { id "ch.hippmann.localizer" version "0.0.1-SNAPSHOT" }
 // won't work  as gradle does not know how to map the plugin id to an actual artifact.
 // this is only required when trying out local builds. Comment this out when trying out a plugin published
 // in the gradle plugin portal.
pluginManagement {
    repositories {
        mavenLocal()
        jcenter()
        gradlePluginPortal()
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.id == "ch.hippmann.localizer") {
            useModule("ch.hippmann:localizer:" + requested.version)
        }
    }
}
```
