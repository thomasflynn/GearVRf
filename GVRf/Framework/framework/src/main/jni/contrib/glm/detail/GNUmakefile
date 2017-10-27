TOPDIR=../../..
OS = linux
ARCH = x86_64

OBJSDIR = $(TOPDIR)/objs/$(OS)/$(ARCH)

SOURCES = \
          dummy.cpp \
          glm.cpp \

INCLUDES = \
		   -I../../ \
		   -I../assimp/include \
		   -I../ \
		   -I$(JAVA_HOME)/include \
		   -I$(JAVA_HOME)/include/linux \



OBJECTS = $(SOURCES:.cpp=.o)

OBJS = $(patsubst %.cpp, $(OBJSDIR)/%.o, $(SOURCES))

LIBDIR = \
		 -L/usr/X11R6/lib \
		 -L/usr/lib/x86_64-linux-gnu/mesa/

LIBS = \
	   -lGL\
	   -lX11\
	   -lm

all: $(OBJSDIR) $(OBJS)

$(OBJSDIR):
	mkdir -p $(OBJSDIR)

$(OBJSDIR)/%.o: %.cpp
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -fPIC -g $< -o $@

-include $(OBJS:.o=.d)

.cpp.o:
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -fPIC -g $< -o $(OBJSDIR)/$@

clean:
	rm -rf $(OBJS) $(OBJSDIR)

clobber: clean
	rm -f $(PROGRAM)


