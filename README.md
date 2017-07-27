# SaTC Distributed Market Model

Simulator for analysis of the cybersecurity implications of 
highly distributed electricity markets.

## Agent

Agent is an abstract class that provides basic features for entities that 
communicate.  It has two subclasses: __Grid__, for agents actually connected
to the power grid, and __Virtual__, for agents that only have communication 
links.

## Channel

A __Channel__ object is communications channel.  An arbitrary number are 
allowed and they can have different properties.  Each agent has a specific 
Channel that it uses to communicate with its upstream parent node.  A Channel 
is used to by one agent to send Msg objects to another.

Each Channel has one main method, __send()__.  It looks up the sender and 
recipient from the corresponding fields of the Msg object it is given and checks 
whether random denial of service filtering applies to the sender.  If so, 
the message is dropped. Otherwise, as long as the message is not diverted (discussed
below) it is passed to the recipient via the recipient's __deliver()__ method.

Three hooks are available to support man in the middle attacks and other 
interventions.  Message diversions can be set up via each channel's 
__divert_to()__ and __divert_from()__ methods. The first diverts all 
messages sent *to* a given node and the second diverts all messages sent
*by* a given node. The *from* diversion is processed first and when both 
apply to a given message it takes precedence.  Messages can be reinserted 
downstream from the diversions via the channel's __inject()__ method.  Future 
features to be implemented:
* Random DOS loss rates that can vary by channel
* VPN channels that prohibit diversions
* Authentication of senders
* Authentication of recipients -- blocking __divert_to()__

## Demand

A __Demand__ object holds a net demand curve expressed as a list of steps.
Positive quantities indicate demand and negative quantites indicate supply.
__Trader__ nodes send __Demand__ objects to __Market__ nodes.  Lower tier 
__Market__ nodes send aggregated __Demand__ objects to higher-tier __Market__ 
nodes.  In all cases the curves are sent via __Msg__ objects.

## Env

The __Env__ object represents the environment under which the simulation
is running.  It includes various global variables and is also responsible
for loading data, configuring the network of __Agent__ nodes, 
the __Channel__ objects they use to communicate, and then starting the
simulation.

## Grid

Abstract class for grid-connected agents (that is, agents through
which power can flow).  Has two subclasses: __Trader__ and __Market__.

## Market

Represents a market.  Aggregates demands by child nodes, which can be 
__Trader__ or other __Market__ nodes. If the node has a parent, it adjusts 
the aggregate demand for transmission costs and capacity to the parent
and passes the adjusted demand up.  If the node does not have a parent, 
it determines the equilibrium price.  Market nodes receive prices from 
upstream, adjust them for transmission parameters, and then pass them 
down to child nodes.

## Msg

A single message from one agent to another.  At the moment, two types of 
messages can be sent: one with a __Demand__ object and one with a 
price.  All __Msg__ objects are passed via __Channel__ objects. Future 
features to be implemented:
* Encryption that prevents reading by diverters
* Signing that prevents spoofing the sender

## Trader

Represents end users or suppliers.  __Trader__ nodes have upstream parents 
that are __Market__ nodes.  They submit __Demand__ objects to their parent
__Market__ nodes and then receive prices back.  Final demand or supply 
results from the prices received.

## Util

A utility class that includes a few general purpose methods for opening
files with built-in exception handling.

## Virtual

Abstract class for agents connected to the communications network but not
connected directly to the power grid.  
