# SaTC Distributed Market Model

Simulator for analysis of the cybersecurity implications of 
highly distributed electricity markets.

## Agent

Agents represent entities that communication.  They include end users or 
electricity suppliers (__Trader__) and market nodes (__Market__).  Market 
nodes are abstract but come in two subclasses that can be instantiated: 
midlevel markets (__Mid__) and root markets (__Root__). 

Trader nodes have upstream parents that are Market nodes.  Mid nodes have
downstream children that are Trader nodes and upstream parents that are 
(for now) Root nodes.  Root nodes have downstream children that are Mid nodes
and do not have upstream parent nodes.

## Channel

A __Channel__ object is communications channel.  An arbitrary number are 
allowed and they can have different properties.  Each agent has a specific 
Channel that it uses to communicate with its upstream parent node.  A Channel 
is used to by one agent to send Msg objects to another.

Each Channel has one main method, __send(Msg)__.  It looks up the sender and 
recipient from the corresponding fields of the __Msg__ object and checks 
whether random denial of service filtering applies to the sender.  If so, 
the message is dropped. If not, the message is passed to the recipient via 
the recipient's __deliver(Msg)__ method.

In subsequent refinements messages may be subject to man in the middle 
interventions or other attacks by passing the message to another node
instead of delivering it to its destination.  The diversion is set 
up via __divert(old_id,new_id)__ to route messages intended for 
old_id to new_id instead.

## Demand

A __Demand__ object holds a net demand curve expressed as a list of steps.
Positive quantities indicate demand and negative quantites indicate supply.
Traders send Demand objects to Mid nodes.  Mid nodes send aggregated
Demand objects to Root nodes.  In all cases the curves are sent via 
Msg objects.

## Env

The __Env__ object represents the environment under which the simulation
is running.  It includes various global variables and is also responsible
for loading data, configuring the network of nodes, and then starting the
simulation.

## Msg

A single message from one agent to another.  At the moment, two types of 
messages can be sent: one with a Demand object and one with a price.  All 
Msg objects are passed via Channel objects.

## Util

A utility class that includes a few general purpose methods for opening
files with built-in exception handling.