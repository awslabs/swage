# swage

Swage is a collection of libraries useful for developing and running services.

Currently it consists of the following Java libraries:
* Metrics
* Type Safe
* Thread Context
* Disseminating Executors


## Metrics

The Swage Metrics library provides a clean, robust API for recording metric
events in a Java application, as well as a core implementation for sending the
instrumented metric data to AWS CloudWatch.

It consists of the following modules:
* metrics-core
* metrics-api

## Type Safe

Swage Type Safe is a small library that provides type-safe solutions that are
not part of the Java standard libraries.  At the moment it consists soley of
the TypedMap, a class for storing heterogenous key/value pairs in a a type safe
way.

It lives in the type-safe module.


## Thread Context

Swage Thread Context is a library to help manage and propagate task context in
an application across threads.  Where a ThreadLocal can help manage context
within one thread, this library provides a way to manage context where a task
may span many threads.

It lives in the thread-context module.


## Disseminating Executors

The Disseminating Executors library solves the same basic problem as the Thread
Context library, but with a different approach.  Ths library is intended to
allow targetd state to cross thread boundaries without invasive changes at
every execution point.  It uses custom task executors to capture and propagate
state.

It lives in the disseminating-executors module.
