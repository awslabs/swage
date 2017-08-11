# State Captures

This library is intended to allow targeted state to cross thread
boundaries without invasive changes at every execution point. In
particular, this enables the developers of framework components the
ability to, with higher confidence than before, transition their
thread-local state through multiple thread pools to some relevant exit
point.

# Example usage

```java
ExecutorService e = Executors.newCachedThreadPool();
e = StateCapture.capturingDecorator(e);
```

# Getting started

For application owners, wrap your `Executors`, `ExecutorService`s
and `CompletionService`s with `StateCapture.capturingDecorator()`.
If passing a lambda to a library that controls its own thread pool,
an application can call `StateCapture.capturingDecorator()` on
the runnable or callable itself.

# Design goals
## Motivating example: tracing

A tracing module to a framework could place the data
they would like to transfer from the entry point to a service to
the other web clients, so tracing identifiers can be propagated over
the wire. Tracing is a system-wide cross-cutting concern, but making
every simple application explicitly wire through all tracing state is
tedious. For applications that are entirely single-threaded, this is
simple: just use an ordinary thread local. Unfortunately, this breaks
at the first use of any executors, commonly used for asynchronous IO.

If the executors are aware of the data that needs to be moved, then
there's no problem - the data can be copied from one thread to the next.
However, modifying all points in execution to make this copying happen
is often a prohibitive amount of work. Making it possible to implement
applications and frameworks in a forward-compatible way for these
scenarios should reduce maintenance costs and long-term burden.

## Motivating example: Logging

Logging setups frequently reference thread-local data to put
information in logs. Log4j 2.x, for example, has both a
"thread context stack" and a "thread context map". Log formats
can refer to variables in these thread context variables.

High quality logs have a huge impact on the operational ability
of teams to identify and fix issues in production, and log metadata
working as expected across thread boundaries helps reduce MTTR.

Log4j 2.x doesn't provide a convenient mechanism to move data across
threads and, even if it did, it would be logging-framework specific.
There is no general solution that multiple logging frameworks have
adopted. A good solution to this problem should be able to work with
any logging framework, including ones not yet identified, or any
other third-party framework with similar requirements. It should not
require the third-party to make invasive changes to enable this;
ideally it would require no changes at all.

## Motivating example: non-thread-safe child objects

Some libraries aren't thread safe, but also have some state that
needs to move from thread to thread. In these cases, a child object
can be created with the required metadata and passed into the next
thread. These objects also sometimes require some kind of cleanup
to occur; this should also be possible.

# Functionality
This library seeks to address all of the motivating scenarios by
being unopinionated about the needs of different libraries and
making it possible for them to implement however fits their world
view. By providing hooks to provide data to pass across a thread
boundary and to execute code before and after executing the real
business task, libraries can do whatever they need to do.

## High-level implementation

At a high level, this library allows state to be captured or created
by a calling thread in a plugin, moved to the scope of a calling thread,
then the plugins are given execution control of the data from within
the called thread.

1. A method like `submit()` is called on a wrapped executor
2. Every plugin is asked for an object representing captured state
3. The callee thread begins execution
4. A before method is called on the object containing the captured state
5. The actual method is called
6. An after method is called on the object containing the captured state

## Limitations

This library is not a silver bullet. For example, if data is
transferred from one thread to another via a shared reference
to a concurrent data structure, there is no opportunity for
captured state to be created or processed. This also cannot transfer
data across other boundaries that do not participate. For example,
a library may have its own thread pool and create its own tasks. This
library does not provide a mechanism to make all thread pools
capture and move state.

# Application integration
## Single-threaded applications
Applications that use a single thread don't need to do anything
## Applications with thread pools
Applications that use thread pools need to wrap all the thread pools
they wish to capture state with a state capturing decorator.
## Applications that use shared data structures to pass data between threads
These applications will need to integrate with their various
frameworks and tools through some manual manner since there
is no automatable mechanism to transfer state between threads
in this case.
# Tool integration

Tools that want to capture and move state with this mechanism must
implement two interfaces: `CapturedState`
and `StateCaptor`. Additionally,
the implemented `StateCaptor` should include in the jar via
the [`ServiceLoader`][1]. To use this, put a text file at
`META-INF/services/StateCaptor`
with each fully qualified implementation of `StateCaptor` on
its own line. These will be picked up and used at runtime. This can also
be achieved using [Google's AutoService][2].

[1]: http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html
[2]: https://github.com/google/auto/tree/master/service
