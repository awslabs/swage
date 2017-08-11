# Context Library

A library to help manage and propagate task context in an application, across
threads.

Often the same context needs to be carried around, but is not strictly tied to a
single thread - one thread might kick of multiple sub threads, or threads may be
reused continuation-style.  So a thread local doesn't always cut it.

## ThreadContext

The ThreadContext class is a holder for context data that can safely carried
across threads.  It can also be tied to the current thread to act as thread-local
data.

The ThreadContext data is presented as a map of heterogenous types, with enum-like
keys.


## ContextAwareExecutor

The ContextAwareExecutor and associated service classes provide an executor-based
interface for handling context data.  Using these makes managing and propagating
the context easy.
