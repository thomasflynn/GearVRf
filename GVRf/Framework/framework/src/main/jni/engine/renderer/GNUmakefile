PROGRAM = librenderer.so

OS = linux
ARCH = x86_64

OBJSDIR = objs/$(OS)/$(ARCH)

SOURCES = \
          batch.cpp \
          batch_manager.cpp \
          gl_renderer.cpp \
          renderer.cpp \
          renderer_instance.cpp \
          vulkan_renderer.cpp \

INCLUDES = \
		   -I../../ \
		   -I../../contrib


OBJECTS = $(SOURCES:.cpp=.o)

OBJS = $(patsubst %.cpp, $(OBJSDIR)/%.o, $(SOURCES))

LIBDIR = \
		 -L/usr/X11R6/lib

LIBS = \
	   -lGL\
	   -lX11\
	   -lm

$(PROGRAM): $(OBJSDIR) $(OBJS)
	$(CXX) $(OBJS) $(LIBDIR) $(LIBS) -o $(PROGRAM)

$(OBJSDIR):
	mkdir -p $(OBJSDIR)

$(OBJSDIR)/%.o: %.cpp
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -g $< -o $@

-include $(OBJS:.o=.d)

.cpp.o:
	$(CXX) $(INCLUDES) -std=c++11 -c -g $< -o $(OBJSDIR)/$@

clean:
	rm -rf $(OBJS) $(OBJSDIR)

clobber: clean
	rm -f $(PROGRAM)


