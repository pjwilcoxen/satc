# SaTC Distributed Market Model

Simulator for analysis of the cybersecurity implications of 
highly distributed electricity markets.

## Adversary

Adversary is an abstract class for virtual agents who try to disrupt the
power grid. It has several subclasses that can perform different
kinds of attacks.

### Adv_Adam

An __Adv_Adam__ object can send false bids to the other nodes inside the grid. 
It looks up the __Intel__ hash map and picks all the target nodes that be 
connected to. Then constructs the false bid and sends the bid to the targets.

### Adv_Beth

__Adv_Beth__ class is the promoted version for __Adv_Adam__ class. It has all 
the functionalities provided by __Adv_Adam__ as well as to forge credentials 
and to decipt the recipient with that.

### Adv_Darth

An __Adv_Darth__ object is able to attack the market with constrained tranmission 
to their upstream nodes. It is supposed to have compromised one trader from the 
target market. It first intercepts all messages sent by the compromised trader. 
Then extracts the trader's historical demands. Shifts the demand curve by the attacker 
customized distance. Finally injects the fake demands back into the channel and these 
demands will be sent to the target market. 

### Adv_Elvira

__Adv_Elvira__ performs similar behavior to __Adv_Darth__. We suppose that __Adv_Elvira__ 
has compromised several traders instead of only one. Then it performs the attack from 
all of these compromised traders, and shifts all their demand curves by a smaller value, 
with a total shift distance that equals to the attacker customized distance. Compared 
with the __Darth__, __Elvira__ performs the attack more softly but with more traders to achieve 
the same result, which means it has less possbility to be discovered by the anomalous data 
detection.

### Adv_Faust

An __Adv_Faust__ object listens the target market channel and intercepts messages 
sent by the target. Then injects the DEMAND message back to the channel without any
modification, but tampers the price info in the PRICE message and injects it back 
to the channel before traders calculate loads. 


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

## History

A __History__ object stores historic price and quantity data for a specific 
agent. Provides several methods used to store and retrieve demand, 
price, quantity, and constraint information. The simulator can load historic 
data from preset history file before it runs. Adversaries can extract 
relevant information from history object to build fake demand curves 
and then perform attacks.

## Intel

Stores a virtual agent's information about another agent within the grid. 
Contains functionality to store an agent's 
grid-level (transmission cost, price, parent, children, tier), 
historic (price, quantity, bid), and some status (compromised, interceptTo, 
interceptFrom, forge, send ,learned) information. Can also 
retrieve max/min/avg information regarding p and q.

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

Abstract class for end agents. __Trader__ nodes have upstream parents
that are __Market__ nodes.  They submit __Demand__ objects 
to their parent __Market__ nodes and then receive prices back. 
Final demand or supply results from the prices received. 

Provides method __getOneDemand()__ to retrieve demand, static method 
__readDraws()__ to load draws from history files, and abstract method 
__drawLoad()__ to build the agent's demand curve.

Has subclass: __TraderMonte__.

### TraderMonte

A __TraderMonte__ object represents the trader under Monte Carlo mode. 
Has two types: end users and suppliers. Implements method __drawLoad()__, 
and provides method __readDraws()__.

## Util

A utility class that includes a few general purpose methods for opening
files with built-in exception handling.

## Virtual

Abstract class for agents connected to the communications network but not
connected directly to the power grid.  
