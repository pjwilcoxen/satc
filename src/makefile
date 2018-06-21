CLASSES = *.java

%.class : %.java 
	javac -cp '../lib/*;.' $*.java

all : market.jar 

market.jar : $(CLASSES:.java=.class) manifest.txt
	jar cvfm $@ manifest.txt \*.class

clean :
	@rm -rvf *.class *.jar *~
