# Releasing infrastructure

--8<--
avito-disclaimer.md
--8<--

We publish releases to Maven Central on demand.

## How to make a new infra release

1. Check if diff against the last release contains any changes for users.
   If not, then probably there are no reasons to make a release.
2. Check the current status of `integration check infra build against avito-android` [Github](http://links.k.avito.ru/N8) or [Stash](http://links.k.avito.ru/integration-check-with-stash)
   1. If it is `Failed` you could release from previous `Succeed` commits or fix problems
   2. If there are pending changes after last success build. You should run build on last changes or use last `success` commit for release.
3. Checkout a `release branch` with a name equals to `projectVersion`. For example, `2022.9`.  
   This branch must be persistent. It is used for automation.
4. Publish infra to internal Artifactory repo [Github](http://links.k.avito.ru/In) or [Stash](http://links.k.avito.ru/publish-infra-from-stash) on the `release branch`
   1. Currently, we don't publish to external Artifactory. [Publish to Maven Central](#publishing-to-maven-central)
5. Update `infraVersion` at an internal avito-android repository
6. Update `infraVersion` at the infra repository:
    - Change `infraVersion` property in the `./gradle.properties` to the new version
    - Bump up a `projectVersion` property in the `./gradle.properties` to the next version
    - Open a pull request with those changes


## Publishing to Maven Central

We publish releases to Maven Central on demand: 
[com.avito.android](https://search.maven.org/search?q=com.avito.android).

??? info "If you release for the first time"

    - [Get an access to Sonatype](#getting-access-to-sonatype)
    - Install [Github CLI](https://cli.github.com)

1. Push `develop` and `release` branches to remote.
1. Manually run [Github publish configuration](http://links.k.avito.ru/releaseAvitoTools) on the latest or needed `release branch`. 
It will upload artifacts to a staging repository in [Sonatype](https://oss.sonatype.org/#stagingRepositories).
So you can upload it in advance at any previous step and drop in case of problems.
1. [Release staging repository](#making-a-release-in-sonatype)
1. Publish a release in Github:  
   ```sh
   make draft_release version=<current release version> prev_version=<last release version>
   ``` 
   You need to have the [`Github cli`](https://github.com/cli/cli).  
   See also more details about [Managing releases in a repository](https://help.github.com/en/github/administering-a-repository/managing-releases-in-a-repository).

### Getting access to Sonatype

1. [Create an account](https://issues.sonatype.org/secure/Signup!default.jspa)
1. Create an issue referencing [original one](https://issues.sonatype.org/browse/OSSRH-64609), asking for `com.avito.android` access
1. Wait for confirmation
1. Login to [nexus](https://oss.sonatype.org) to validate staging profile access

Some additional info:

- [Maven central publishing reference article](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/)

### Making a release in Sonatype

We publish a release through a temporary staging repository. 
If something goes wrong you can drop the repository to cancel the publication process.

1. Open [Staging repositories](https://oss.sonatype.org/#stagingRepositories)
![oss-avito](https://user-images.githubusercontent.com/1104540/109542777-92d5b080-7ad6-11eb-9393-30adfa11f749.png)
In a Content tab you can see uploaded artifacts.
1. Close the repository:  
You don’t need to provide a description here.
![oss-close](https://user-images.githubusercontent.com/1104540/109543602-8ef65e00-7ad7-11eb-850d-70542451ee94.png)
In an Activity tab you can track progress.
![oss-release](https://user-images.githubusercontent.com/1104540/109543639-9ae22000-7ad7-11eb-82d4-d3d2c1975521.png)
1. Release the repository. It will publish the contents to Maven Central
![oss-release-confirm](https://user-images.githubusercontent.com/1104540/109543687-ac2b2c80-7ad7-11eb-8294-7d603c523156.png)
1. Wait till new packages appear on Maven Central. It takes usually about 15-30 min.
   You can see them earlier in a [repository manager](https://oss.sonatype.org/#nexus-search;quick~com.avito.android) 
   or in [public search](https://search.maven.org/search?q=com.avito.android) before all of them will be available for download.

Some additional info:

- [Maven central publishing reference article](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/)

## Local integration tests against Avito

1. Run `make publish_to_maven_local` in github repository.
1. Run integration tests of your choice in avito with `local` version of infrastructure.

## CI integration tests against Avito

1. Choose configuration from [existed](#ci-integration-configurations)
1. Run build.  
   If you need to test unmerged code, select a custom build branch.  
   You will see branches from both repositories:

![](https://user-images.githubusercontent.com/1104540/75977180-e5dd4d80-5eec-11ea-80d3-2f9abd7efd36.png)

- By default, build uses develop from github against develop from avito
- If you pick a branch from avito, it will run against develop on github
- If you pick a branch from github, it will run against develop on avito
- To build both projects of special branch, they should have the same name

## CI integration configurations

- [fast check configuration (internal)](http://links.k.avito.ru/fastCheck) - pull request's builds
- [integration check](http://links.k.avito.ru/A8) - currently, contains the biggest amount of integration checks
- [nightly integration check](http://links.k.avito.ru/gZ) - the same as `integration check` but uses more Android emulators
