# Swage Metrics API

The Swage Metrics library provides a clean, robust API for recording metric events in a Java
application.  It is based on learnings from previous and existing metric
systems widely used within Amazon.

Implementation(s) of the API, and additional metric-related functionality live
in the swage-metrics-core module.


# Design Goals

* Simple.
* Performant.
* Robust.

Simple means code that wants metrics should get correct metrics with little or
no effort.  The core API is streamlined for the common case, but allows more
complex cases to be built on top of it.

Performant means instrumented systems should see little or no impact from
metrics collection.  The API defines cheap, non-blocking operations and lends
itself to fast implementations free of excessive garbage creation.

Robust means that if you are using the API you are using it correctly - there
are no gotchas.  Complex systems can be built using the API, but using it for
complex things does not lead to complex problems.



# Metrics

A metric represents a data point at a specific, well defined granularity.
Measurements are taken, these measurements are aggregated (or not) into a
Metric.  For example: take 10 cpu measurements during a minute and produce
1-minute cpu utilization metric.

Measurement datum are defined in terms of metric events - something occurred
and must be recorded.  These are instrumented traits of the running system,
which in aggregate give metrics about the system.

Metrics exist to provide information and introspection on an application.
There are three parts:
* Instrumenting the application to emit metric data.
* Capturing and recording the instrumented data.
* Gathering and aggregating the data to provide actionable information.

The metrics API is concerned with the first part: instrumenting java code
to emit metric data.  Implementations sitting behind the API take care of the
latter parts.

Many solutions marry the data collection with the data interpretation.  This
clouds things, makes solutions slower and more complicated, and reduces options
for different iterpretation.  This API takes the stance that code
recording a metric should not care how the metric is interpreted, it should
just be able to spit out the info and move on.


## Instrumenting

The first concern is instrumenting to emit data, such that it can then be
captured in a way that can be interpreted.  There are two pieces to this:
the actual recording of an event, and a way to handle the context in which the
event occurs, for proper aggregation/interpretation.  This library provides an
API with easy mechanisms for recording in a given context and for managing
that context.


## Recording

A metrics solution has to provide a way to record the data in such a way that
a) recording does not affect the instrumented code and b) it can be surfaced
in interpretation tools.  The metrics API allows for different recordin
schemes to be used without changes to the application - the choice of a
recording solution is up to the implementation of the API.

For performance purposes the recording layer might do some aggregation,
sampling, or other computations before the data is sent to the interpretation
system(s).


## Aggregating

Once the application is instrumented and the data recorded it needs to be able
to be viewed and interpreted.  It is assumed that any interpretation of metrics
is done with some external tooling, either viewing the data directly from where
it is recorded or pulling into an internal system for examination.

Similar to recording, how data is aggregated is left up to the API
implementation.  This separates out the concern of interpreting the metric
event data from the gathering of such data, simplifying the instrumentation of
an application.


# API

This covers the recording of measurements, aka metric events. It explicitly
focuses on when a metric event occurs, leaving out any aggregation or
interpretation of the event.

When aggregation and interpretation are left out, there is only really one
operation needed: record the event.  The metrics API provides two distinct ways
to do this, depending on the type of event:

```java
    void record(
        Metric label,
        Number value,
        Unit unit,
        Instant time,
        TypedMap context);

    count(
        Metric label,
        long delta,
        TypedMap context);
```

The record method is the general workhorse, for reporting a value as measured
at a specific time.  The count method provides a way to record a change of
value, independent of the instantaneous value itself - errors encountered,
tasks started/stopped, etc.

If you need to distinguish between occurrences of an event, or care that an
event did not occur (for ratios of success for example) then you want to use
the record method.  If you are aggregating values and only care that a value
changed, use the count method.


## Metric Labels

One of the weaknesses of previous metrics solutions was that of metric names
modeled as free-form strings. Consuming systems have limits on the usable
values for metric names, and there are expectations and standards around what
names to use for what.

This library uses a Metric abstraction to address this.  It replaces the
freeform String of a metric name. Names used for recording metrics are thus
limited to a predefined subset, solving fat-fingering and helping ensure values
go where they should. The Metric class also provides validation to ensure that
the name conforms to a reasonable format. Intent is to capture common metrics
in a strongly typed, correctly defined way, and let users add new values as needed.


## Context

Every event that occurs in a system has a context associated with it.  For a
service this might be the request context, or for a reactive system it might be
a task being executed.  When emmitting a metric event, information about this
context needs to be included for proper aggregation and filtering of the data.

The metrics API is designed with context as an explicit first-class concept.
An application (or the framework it is built on) may have any number of ways to
manage context; the metrics API uses a typesafe heterogenous bag of data
containing the metrics-relevant metadata required to uniquely identify the
context.

For ease of use of the API, this is provided in a Context object.  The Context
object contains an arbitrary TypedMap object and a reference to the
MetricRecorder to be used.  It is expected that a user of metrics will need to
reference the MetricRecorder from wherever they also have a metric context, so
the Context object is defined in terms of the recorder.  The MetricRecorder
acts as a 'factory' for the metric context - as a Context is always associated
with a MetricRecorder, and one presumably has a recorder in scope when needed,
it is thus convenient to create and use new contexts as required.  Convenience
methods exist on the Context to record using the implicit MetricRecorder with
the given context.

In general, code that emits metric events only needs to have and use a Context
object.



## Metric Recorder

The record and count instrumentation methods exist on a MetricRecorder object.
When recording a metric event, the reporter is called with the metric and the
context.  The implementation of the MetricRecorder used determines what happens
to the metric event - it may be written to a log, sent to a downstream system,
or perhaps aggregated in process.

A MetricRecorder is usable throughout an application, being thread-safe and
independent of any particular context.  It is expected that normally one
instance will be shared for a java process.  This avoids (re)creating multiple
objects per task/request every time a metric event occurs, and centralizes any
coordination or locking that must occur when writing out the events.

The underlying MetricRecorder API is stateless, with each record or count call
taking in both the event and the context it occured in.  However, we provide a
wrapping Context object to carry both the context metadata and the recorder to
use for recording - a Context object is created for each logical metric
context, and code within that context need only refrence the Context object to
record metric events.


# Example


```java
// During app initialization an appropriate recorder implementation is created.
MetricRecorder recorder = new RecorderImpl();

// Sometime later, per task/request/operation, create an object that captures
// the context and will be provided to the task code.
TypedMap contextData = DomainSpecificBuilder
                            .withId(taskId)
                            .somethingElse(somethingElse)
                            .build();
SomeContext metricCtx = recorder.context(contextData);

// Inside task/request/operation, record some metrics
metricCtx.record(StandardMetric.Time, sometime, Unit.MILLISECOND);
```
