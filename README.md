# SaTC Distributed Market Model

Simulator for analysis of the cybersecurity implications of 
highly distributed electricity markets.

## Agent

Agent is an abstract class that provides basic features for entities that 
communicate.  One subclass, __Trader__, can be instantiated to represent 
end users or suppliers.  A second subclass, __Market__, is an abstract 
class that provides basic capabilities for market nodes.  It has two 
subclasses that can be instantiated: __Mid__, which represents midlevel 
markets, and __Root__, which represents root markets.

__Trader__ nodes have upstream parents that are __Mid__ nodes.  __Mid__ 
nodes have downstream children that are __Trader__ nodes and upstream 
parents that are (for now) __Root__ nodes.  __Root__ nodes have downstream 
children that are __Mid__ nodes and do not have upstream 
parent nodes.

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

Man in the middle attacks and other interventions can be set up via each
channel's __divert(old_id,new_id)__ method.  When a diversion is present,
messages originally intended for old_id are rerouted to new_id instead.
Future features to be implemented:
* Allow copy of messages rather than fully diverting them
* Random DOS loss rates that can vary by channel
* VPN channels that prohibit diversions

## Demand

A __Demand__ object holds a net demand curve expressed as a list of steps.
Positive quantities indicate demand and negative quantites indicate supply.
__Trader__ nodes send __Demand__ objects to __Mid__ nodes.  __Mid__ nodes 
send aggregated __Demand__ objects to __Root__ nodes.  In all cases the 
curves are sent via __Msg__ objects.

## Env

The __Env__ object represents the environment under which the simulation
is running.  It includes various global variables and is also responsible
for loading data, configuring the network of __Agent__ nodes, 
the __Channel__ objects they use to communicate, and then starting the
simulation.

## Msg

A single message from one agent to another.  At the moment, two types of 
messages can be sent: one with a __Demand__ object and one with a 
price.  All __Msg__ objects are passed via __Channel__ objects. Future 
features to be implemented:
* Encryption that prevents reading by diverters
* Signing that prevents spoofing the sender

## Util

A utility class that includes a few general purpose methods for opening
files with built-in exception handling.