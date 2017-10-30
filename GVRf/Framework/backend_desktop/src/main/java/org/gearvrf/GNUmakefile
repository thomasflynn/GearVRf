CLASSDIR = .

JFLAGS = -d .

JARPATHS = /home/flynnt/trees/lwjgl/lwjgl-opengl/lwjgl-opengl.jar:/home/flynnt/trees/lwjgl/lwjgl-opengl/lwjgl-opengl-natives-linux.jar:/home/flynnt/trees/lwjgl/lwjgl-glfw/lwjgl-glfw.jar:/home/flynnt/trees/lwjgl/lwjgl-glfw/lwjgl-glfw-natives-linux.jar:/home/flynnt/trees/lwjgl/lwjgl/lwjgl.jar:/home/flynnt/trees/lwjgl/lwjgl/lwjgl-natives-linux.jar

.SUFFIXES: .java .class

SRCS = \
	    DesktopActivityDelegate.java \
		DesktopRenderer.java \
		DesktopViewManager.java \
		DesktopXMLParser.java 

CLASSES = $(SRCS:.java=.class)

default: $(CLASSDIR) $(CLASSES)

JARS = $(subst :, ,$(JARPATHS))

$(CLASSDIR):
	mkdir classes

ORG=$(CLASSDIR)/org/gearvrf/

.java.class:
	CLASSPATH=$(JARPATHS) javac $(JFLAGS) $<

jar: $(CLASSDIR) $(CLASSES)
	echo "Manifest-Version: 1.0" > manifest.txt
	echo "Class-Path: $(JARS)" >> manifest.txt
	echo "Main-Class: org.gearvrf.DesktopActivityDelegate" >> manifest.txt
	echo "" >> manifest.txt
	jar -cmf manifest.txt desktop.jar $(ORG)/*.class

clean:
	rm -rf $(ORG) manifest.txt

