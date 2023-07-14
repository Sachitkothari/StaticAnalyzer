TARGET=part1
JFLAGS= -g
JC = javac
JVM = java
.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = 	srcPartC.java
	
default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) -f *.class

run:
		java srcPartC