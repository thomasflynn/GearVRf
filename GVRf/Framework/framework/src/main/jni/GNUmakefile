OS = linux
ARCH = x86_64

OBJSDIR = objs/$(OS)/$(ARCH)
OUTDIR = output/$(OS)/$(ARCH)


SUBDIRS = \
		  contrib \
		  eglextension \
		  engine \
		  gl \
		  objects \
		  shaders \
		  util \
		  vulkan \

SOURCES = \
	buildinfo.cpp \
	configuration_manager.cpp \
	configuration_manager_jni.cpp \
	view_manager_jni.cpp \

SHADERC ?= ../../../../../../../../shaderc
#SHADERC ?= /home/flynnt/trees/shaderc

ALLSRCS := $(shell find -iname '*.cpp')
FILENAME = $(notdir $(ALLSRCS))
DIRNAME = $(dir $(ALLSRCS))
OUTOBJS = $(addsuffix $(OS)/$(ARCH)/, $(DIRNAME))
ALLOBJS = $(patsubst %.cpp,$(OS)/$(ARCH)/%.o,$(ALLSRCS))

INCLUDES = \
		   -I./ \
		   -I./contrib \
		   -I$(JAVA_HOME)/include \
		   -I$(JAVA_HOME)/include/linux \



OBJECTS = $(SOURCES:.cpp=.o)

OBJS = $(patsubst %.cpp, $(OBJSDIR)/%.o, $(FILENAME))

LIBDIR = \
		 -L/usr/X11R6/lib \
		 -L/usr/lib/x86_64-linux-gnu/mesa/

LIBS = \
	   -lGL\
	   -lX11\
	   -lm

all: $(OBJSDIR) $(OUTDIR) $(OBJECTS) $(SUBDIRS)
	$(CXX) -shared $(OBJS) $(LIBDIR) $(LIBS) -o $(OUTDIR)/libgvrf.so

.PHONY: subdirs $(SUBDIRS)

subdirs: $(SUBDIRS)

$(SUBDIRS): all
		$(MAKE) -C $@


$(OBJSDIR):
	mkdir -p $(OBJSDIR)

$(OUTDIR):
	mkdir -p $(OUTDIR)

$(OBJSDIR)/%.o: %.cpp
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -MMD -fPIC -g $< -o $@

-include $(OBJS:.o=.d)

.cpp.o:
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -fPIC -g $< -o $(OBJSDIR)/$@

clean: 
	rm -rf $(OBJS) $(OBJSDIR); \
	for dir in $(SUBDIRS); do \
		$(MAKE) -C $$dir clean; \
	done
	
