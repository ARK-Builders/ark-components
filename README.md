# **ARK Android**

ARK Android contains a set of independent component libraries which can be used across ARK projects.

## How to release a new version of a module

```
git tag <module_name>.v<major>.<minor>.<patch>
git push --tags

```

Example :

```
git tag tagselector.v0.0.9-SNAPSHOT-01
git push --tags

```


## Note when adding a new module

- Create an action build file under <.github/workflows/> folder, following any existing build script.
- Create a release build file under <.github/workflows/> folder, following any existing release script.

