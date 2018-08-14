# Contributing

:+1: :tada: Thanks for taking the time to contribute! :tada: :+1:

The following is a set of guidelines for contributing to John Deere and its packages.

***For the short term, we are not accepting pull requests as we work on a process to accept contributions from the community.***

We expect to begin taking contributions before 2019 begins.

In the meantime, here are some ways you should prepare your contributions for when they can be accepted when we are ready.
* All changes must be test-driven.
* Run ```mvn clean verify``` and ensure no failures or warnings.
* Follows the [Semantic Versioning](https://semver.org/) rules.
* There should be discussion via an Issue before breaking changes are developed.
* Expect that before a pull request is accepted, all owners of the contributed copyrighted work (including companies) 
will have signed a Contributor License Agreement.
* All contributions must adhere to our [Copyright Policy](./COPYRIGHT_POLICY.md).

## Code of Conduct

This project adheres to a [code of conduct](./CODE_OF_CONDUCT.md).

By participating, you are expected to uphold this code.

## Issues

Please review existing issues before creating a new one.

If it ***has*** already been reported, add a comment to the existing issue instead of creating a new one.

If it ***has not*** already been reported, create a new issue using the [issue template](./ISSUE_TEMPLATE.md), using a clear and descriptive title.

## Pull Requests

Please use the [pull request template](./PULL_REQUEST_TEMPLATE.md) when creating a new pull request.

### Etiquette / Rules of Engagement

We try to follow [GitHub's "How to Write the Perfect Pull Request"](https://github.com/blog/1943-how-to-write-the-perfect-pull-request), for both approach and feedback, to increase responsiveness.
 
In short, we try and adhere to the following etiquette / rules of engagement.

* Please keep your commits and PRs as small and logically grouped as possible!
* Fire off a "heads up" issue or PR so owners can be prepared and start thinking and / or buying into the implementation.
* PR issuers / creators can `@` mention owners if they have heard no reply within the expected timeframe.

### Visual Indicators
 
To help distinguish between "must fix" and "just so you know / have you thought about" comments.

* ***"Must Fix"***
  * Pull request **will not be merged*** without addressing
  * :x: `:x:`
* ***"Just So You Know..."*** or ***"Have You Thought About?"***
  * Pull request **can be merged** without addressing, but a conversation is *highly* encouraged. Changes can still happen with these comments, but they are not required.
  * :bulb: `:bulb:`

### Automation Over Nitpicking

In general, if it passes the automated build and security checks, you're good to go!

However, things like design, refactoring, semantic HTML, etc. probably can't be automated easily, so those are still valid code review comments to leave.

### Timeliness

To account for the maintainer potentially being on vacation, please give two weeks for a review.

### Verification

All pull request must be able to have a passing build, if enabled, before they will be accepted.

Please see the project [README](../README.md) for instructions on how to test locally.

### Privacy
See the [Privacy Statement](PRIVACY.md).
