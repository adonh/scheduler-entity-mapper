# mapper

This is an open source project!

[![Build Status](https://magnum.travis-ci.com/PagerDuty/???)](https://magnum.travis-ci.com/PagerDuty/mapper)

## Description

Entity-mapper is a simple API for writing annotated classes into mutation batch, and converting query results back into class instances. Entity-mapper API is database and driver agnostic.

Key features:
 * Native handling of Scala Options
 * Nested entities
 * STI style inheritance support


## Installation

Make sure your project has a resolver for the PagerDuty artifactory repository, you can then add the dependency to your SBT build file:

```scala
libraryDependencies += "com.pagerduty" %% "mapper" % "0.4.3"
```

## Contact

This library is primarily maintained by the Core Team at PagerDuty.

## Contributing

Contributions are welcome in the form of pull-requests based on the master branch.

We ask that your changes are consistently formatted as the rest of the code in this repository, and also that any changes are covered by unit tests.


## Changelog

See [CHANGELOG.md](./CHANGELOG.md)
