Underground
===========

What it is
----------

Underground is an attempt to build a HA messaging broker, inspired
by LMAX' disruptor, Kafka, and other stuff. It is written in Scala.

The architecture is simple: messages flow in, are handed off to a
disruptor, which is setup to replicate to the slave and persist to
the disk first, then send it to the actual message broker logic.
The message broker spits out responses to a second disruptor, which
is wired up to transmit them on the network on the master and do
nothing on the slave. In this way, master and slave stay in sync.

Scaling is currently no concern. A single setup should be able to
handle most messaging requirements, the target is to be able to
saturate at least one 1GigE port.

How it is built
---------------

It is fully test-driven, using Scalatest with JMock. HA is built in
from the start, not slapped on as an afterthought as many messaging
products seem to have been done. HA also will be fully tested.

The test-driven philosphy also applies to performance. We will start
simple, write performance tests (see the perf/ subproject), and
prove performance improvements before applying them.

Status
------

Not even alpha :)

