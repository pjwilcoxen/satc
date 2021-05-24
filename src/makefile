#  makefile for market.jar
#  note: requires gnu make extensions

CLASSES = *.java

SEP = :
ifeq ($(OS),Windows_NT)
    SEP = ;
else ifeq ($(shell uname),Linux)
    SEP = ;
endif

%.class : %.java 
	javac -cp "../lib/*$(SEP)." $*.java

all : market.jar 

market.jar : $(CLASSES:.java=.class) manifest.txt
	jar cvfm $@ manifest.txt *.class

clean :
	@rm -rvf *.class *.jar *~
