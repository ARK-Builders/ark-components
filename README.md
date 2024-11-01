# **ARK Android**

ARK Android contains a set of independent component libraries which can be used across ARK projects.

## How to release a new version of a module

1. Create branch
```
git checkout -b release/<module_name>_<major>.<minor>.<patch>
```
2. Replace version string manually in module `build.gradle.kts` (`scmVersion.version` always provides "-SNAPSHOT" suffix)
```
val libVersion: String = scmVersion.version
val libVersion: String = "<major>.<minor>.<patch>"
```
3. Commit new version
```
git add <module_name>/build.gradle.kts
git commit -m "Bump <module_name> to <major>.<minor>.<patch>"
```
4. Tag commit and push
```
git tag <module_name>.v<major>.<minor>.<patch>
git push --tags
```

## Note when adding a new module

- Create an action build file under `.github/workflows/` folder, following any existing build script.
- Create a release build file under `.github/workflows/` folder, following any existing release script.

## Making the Storage Demo work

This section guides you through setting up and running the Storage Demo subpage.

**1. Download the `fs-storage` JNI Libraries:**

The demo requires JNI libraries (libs).  Download these from the following location:

* **[ark-core repository](https://github.com/ARK-Builders/ark-core)**

    - If you can't find them in the "Releases" section, check the latest successful build actions for artifacts.

**2. Place the Libraries:**

After downloading, move the JNI library files into your project's `sample/src/main/jniLibs` directory. **If the path doesn't exist, create it**

Your project structure should resemble this:

```
...
sample/
    ...
    src/
        main/
            ...
            jniLibs/
                arm64-v8a/  
                armeabi-v7a/
                x86/
                x86_64/
            ...
        ...
    ...
```

With the `fs-storage` JNI libraries in place, you're ready to build, run the project and use the Storage Demo subpage.
