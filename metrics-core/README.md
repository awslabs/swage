# Swage Metrics Core Implementation

Core implementations of the Swage Metrics API, along with additional functionality for
metric related tasks.


# StandardContext

A convenience class for context data common across many different applications.
Contains a few standard keys, and is itself a builder for a TypedMap containing
data for said keys.


# StandardMetric

A convenience class defining metrics common across many different applications.
Users of Swage Metrics may use these pre-defined metrics rather than (re)defining ones
specific to the application.


# Recorder

Some basic, standard MetricRecorder implementations are provided.

## CloudWatchRecorder

The CloudWatchRecorder is the 'workhorse' MetricRecorder implementation, for
use in applications that send their metric data to AWS CloudWatch.  This
recorder will aggregate and batch metric events, sending to CloudWatch as the
downstream data store.  Requires a properly configured AWS account, with an AWS
SDK present and authenticated at runtime.

CloudWatch uses the concept of 'dimensions' for filtering metric event data,
but all filtering decisions are made prior to sending the data to CloudWatch.
The CloudWatchRecorder accepts dimension configuration in the form of a
DimensionMapper, which will transform metric context data to CloudWatch
dimensions appropriate for filtering.

## FileRecorder

The FileRecorder provides a simple way of writing metric events to a file.  It
is included as an example, and does not write to any known or usable format.
Future work may expand this with support for various data formats, but as of
initial release this recorder is not intended be used in a production system.


# Measures

The basic metrics API provides a simplified, clean way to record metric
events, but it is agnostic on how event data is gathered.  Here we provide
easy ways to instrument code with actual measures.

As of initial release this is very bare bones, will be expanded upon in the
future.


# JMX

For metric data related to the running system, independent of metric-causing
events, a periodic polling system is provided.  This takes the form of an
MXBeanPoller, which will read a set of sensors at a set interval and report
their metric information to the configured MetricRecorder.  Multiple different
sensors are provided to gather information on the current state of the
application via JMX.


# Scoped

Inherent in the metrics API is the concept of context - proper use of the API
requires carrying around the context wherever metrics need to be recorded.
This can be burdensome on appliction developers, so we provide a mechanism to
handle this automatically.

Using a variant of the ThreadLocal concept, ThreadContext, the ScopedMetrics
class can be used to implicitly carry around a metric context object.  The
context then need only be referenced once, with nested code only needing refer
to the ScopedMetrics static methods.

This requires the framework/application scaffolding code to properly handle the
ThreadContext when transitioning tasks and threads, but frees the majority of
application code from having to explicitly worry about context.
