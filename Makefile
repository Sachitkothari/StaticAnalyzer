TARGET=part1
JFLAGS= -g
JC = javac
JVM = java
.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = 	src.java
	
default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) -f *.class

run:
		java src