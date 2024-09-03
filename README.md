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
git push
```
4. Tag commit and push
```
git tag <module_name>.v<major>.<minor>.<patch>
git push --tags
```

## Note when adding a new module

- Create an action build file under `.github/workflows/` folder, following any existing build script.
- Create a release build file under `.github/workflows/` folder, following any existing release script.

