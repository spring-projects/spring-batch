Contributor Guidelines
======================

Have something you'd like to contribute to **Spring Batch**? We welcome pull requests, but ask that you carefully read this document first to understand how best to submit them; what kind of changes are likely to be accepted; and what to expect from the Spring team when evaluating your submission.

Please refer back to this document as a checklist before issuing any pull request; this will save time for everyone!

## Code of Conduct

Please see our [code of conduct](https://github.com/spring-projects/.github/blob/master/CODE_OF_CONDUCT.md).

## Reporting Security Vulnerabilities

Please see our [Security policy](https://github.com/spring-projects/spring-batch/security/policy).

## Understand the basics

Not sure what a *pull request* is, or how to submit one?  Take a look at the excellent [GitHub help documentation][] first.

## Search Github issues first; create an issue if necessary

Is there already an issue that addresses your concern?  Do a bit of searching in [Github issues][] to see if you can find something similar. If not, please create a new issue before submitting a pull request unless the change is truly trivial, e.g. typo fixes, removing compiler warnings, etc.

## Sign the contributor license agreement

If you have not previously done so, please fill out and
submit the [Contributor License Agreement](https://cla.pivotal.io/sign/spring).

## Fork the Repository

1. Go to [https://github.com/spring-projects/spring-batch](https://github.com/spring-projects/spring-batch)
2. Hit the "fork" button and choose your own github account as the target
3. For more details see [https://help.github.com/fork-a-repo/](https://help.github.com/fork-a-repo/)

## Setup your Local Development Environment

1. `git clone git@github.com:<your-github-username>/spring-batch.git`
2. `cd spring-batch`
3. `git remote show`
_you should see only 'origin' - which is the fork you created for your own github account_
4. `git remote add upstream git@github.com:spring-projects/spring-batch.git`
5. `git remote show`
_you should now see 'upstream' in addition to 'origin' where 'upstream' is the *spring-projects*repository from which releases are built_
6. `git fetch --all`
7. `git branch -a`
_you should see branches on origin as well as upstream, including 'master'_

## A Day in the Life of a Contributor

* _Always_ work on topic branches (Typically use the Github issue ID as the branch name).
  - For example, to create and switch to a new branch for issue BATCH-123: `git checkout -b BATCH-123`
* You might be working on several different topic branches at any given time, but when at a stopping point for one of those branches, commit (a local operation).
* Please follow the "Commit Guidelines" described in this chapter of Pro Git: [https://git-scm.com/book/en/v2/Distributed-Git-Contributing-to-a-Project](https://git-scm.com/book/en/v2/Distributed-Git-Contributing-to-a-Project#_commit_guidelines)
* Then to begin working on another issue (say BATCH-101): `git checkout BATCH-101`. The _-b_ flag is not needed if that branch already exists in your local repository.
* When ready to resolve an issue or to collaborate with others, you can push your branch to origin (your fork), e.g.: `git push origin BATCH-123`
* If you want to collaborate with another contributor, have them fork your repository (add it as a remote) and `git fetch <your-username>` to grab your branch. Alternatively, they can use `git fetch --all` to sync their local state with all of their remotes.
* If you grant that collaborator push access to your repository, they can even apply their changes to your branch.
* When ready for your contribution to be reviewed for potential inclusion in the master branch of the canonical *spring-batch* repository (what you know as 'upstream'), issue a pull request to the *spring-projects* repository (for more detail, see [GitHub help documentation][]).
* The project lead may merge your changes into the upstream master branch as-is, he may keep the pull request open yet add a comment about something that should be modified, or he might reject the pull request by closing it.
* A prerequisite for any pull request is that it will be cleanly merge-able with the upstream master's current state. **This is the responsibility of any contributor.** If your pull request cannot be applied cleanly, the project lead will most likely add a comment requesting that you make it merge-able. For a full explanation, see the Pro Git section on rebasing: [https://git-scm.com/book/en/v2/Git-Branching-Rebasing](https://git-scm.com/book/en/v2/Git-Branching-Rebasing). As stated there: "> Often, you’ll do this to make sure your commits apply cleanly on a remote branch — perhaps in a project to which you’re trying to contribute but that you don’t maintain."

## Keeping your Local Code in Sync
* As mentioned above, you should always work on topic branches (since 'master' is a moving target). However, you do want to always keep your own 'origin' master branch in synch with the 'upstream' master.
* Within your local working directory, you can sync up all remotes' branches with: `git fetch --all`
* While on your own local master branch: `git pull upstream master` (which is the equivalent of fetching upstream/master and merging that into the branch you are in currently)
* Now that you're in synch, switch to the topic branch where you plan to work, e.g.: `git checkout -b BATCH-123`
* When you get to a stopping point: `git commit`
* If changes have occurred on the upstream/master while you were working you can synch again:
    - Switch back to master: `git checkout master`
    - Then: `git pull upstream master`
    - Switch back to the topic branch: `git checkout BATCH-123` (no -b needed since the branch already exists)
    - Rebase the topic branch to minimize the distance between it and your recently synched master branch: `git rebase master`
(Again, for more detail see the Pro Git section on rebasing: [https://git-scm.com/book/en/v2/Git-Branching-Rebasing](https://git-scm.com/book/en/v2/Git-Branching-Rebasing))
* **Note** You cannot rebase if you have already pushed your branch to your remote because you'd be rewriting history (see **'The Perils of Rebasing'** in the article). If you rebase by mistake, you can undo it as discussed [in this stackoverflow discussion](https://stackoverflow.com/questions/134882/undoing-a-git-rebase). Once you have published your branch, you need to merge in the master rather than rebasing.
* Now, if you issue a pull request, it is much more likely to be merged without conflicts. Most likely, any pull request that would produce conflicts will be deferred until the issuer of that pull request makes these adjustments.
* Assuming your pull request is merged into the 'upstream' master, you will actually end up pulling that change into your own master eventually, and at that time, you may decide to delete the topic branch from your local repository and your fork (origin) if you pushed it there.
    - to delete the local branch: `git branch -d BATCH-123`
    - to delete the branch from your origin: `git push origin :BATCH-123`

## Maintain a linear commit history

When issuing pull requests, please ensure that your commit history is linear. From the command line you can check this using:

````
log --graph --pretty=oneline
````

As this may cause lots of typing, we recommend creating a global alias, e.g. `git logg` for this:

````
git config --global alias.logg 'log --graph --pretty=oneline'
````

This command, will provide the following output, which in this case shows a nice linear history:

````
* e1f6de38e04a5227fea2d4df193a5b50beaf2d00 BATCH-2002: Initial support for complex conditional replacements
* 65d2df652abaae2ca309d96e3026c2d67312655f Add ability to set a custom TaskExecutor impl, remove unused namespaces from JSR bootst
* 85807568575c24d8878ad605a344f2bc35bb2b13 Update to allow restart parameters to override previous parameters in JsrJobOperator an
* a21df75ce9dfc92e9768353b827da4248aefe425 BATCH-2049: Support multiple fragmentRootElementNames in StaxEventItemReader
* 7f1130c9a265a3ce18a46cbbc122e6573167a036 Fix TCK test JobOperatorTests.testJobOperatorRestartJobAlreadyAbandoned
* c4231c4cc861bbcc43437c80a03ddd9b7b2897a3 Fixed no executions returned check and added a unit test
````
If you see intersecting lines, that usually means that you forgot to rebase you branch. As mentioned earlier, **please rebase against master** before issuing a pull request.

## Code style

Please carefully follow the same [code style as Spring Framework](https://github.com/spring-projects/spring-framework/wiki/Code-Style).

## Add Apache license header to all new classes

```java
/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ...;
```

## Update license header to modified files as necessary

Always check the date range in the Apache license header. For example, if you've modified a file in 2013 whose header still reads

```java
 * Copyright 2002-2011 the original author or authors.
```

then be sure to update it to 2013 appropriately

```java
 * Copyright 2002-2013 the original author or authors.
```

## Use @since tags

Use @since tags for newly-added public API types and methods e.g.

```java
/**
 * ...
 *
 * @author First Last
 * @since 3.0
 * @see ...
 */
```

## Submit JUnit test cases for all behavior changes

Search the codebase to find related unit tests and add additional @Test methods within. It is also acceptable to submit test cases on a per Github issue basis.

## Squash commits

Use `git rebase --interactive`, `git add --patch` and other tools to "squash" multiple commits into atomic changes. In addition to the man pages for git, there are many resources online to help you understand how these tools work. Here is one: https://book.git-scm.com/book/en/v2/Git-Tools-Rewriting-History#_squashing .

## Use your real name in git commits

Please configure git to use your real first and last name for any commits you intend to submit as pull requests. For example, this is not acceptable:

    Author: Nickname <user@mail.com>

Rather, please include your first and last name, properly capitalized, as submitted against the Spring contributor license agreement:

    Author: First Last <user@mail.com>

This helps ensure traceability against the CLA, and also goes a long way to ensuring useful output from tools like `git shortlog` and others.

You can configure this globally via the account admin area GitHub (useful for fork-and-edit cases); globally with

    git config --global user.name "First Last"
    git config --global user.email user@mail.com

or locally for the *spring-batch repository only by omitting the '--global' flag:

    cd spring-batch
    git config user.name "First Last"
    git config user.email user@mail.com

## Run all tests prior to submission

See the [checking out and building][] section of the README for instructions. Make sure that all tests pass prior to submitting your pull request.

## Mention your pull request on the associated Github issue

Add a comment to the associated Github issue(s) linking to your new pull request.

[GitHub help documentation]: https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/about-pull-requests
[Github issues]: https://github.com/spring-projects/spring-batch/issues
[checking out and building]: https://github.com/spring-projects/spring-batch#building-from-source
