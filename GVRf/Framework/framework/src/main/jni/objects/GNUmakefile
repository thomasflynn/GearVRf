OS = linux
ARCH = x86_64

OBJSDIR = objs/$(OS)/$(ARCH)


SUBDIRS = \
		  components \
		  textures \

.PHONY: subdirs $(SUBDIRS)

subdirs: $(SUBDIRS)

$(SUBDIRS): all 
		$(MAKE) -C $@

SOURCES = \
	bounding_volume.cpp \
	data_descriptor.cpp \
	hybrid_object_jni.cpp \
	index_buffer.cpp \
	index_buffer_jni.cpp \
	light.cpp \
	light_jni.cpp \
	mesh.cpp \
	mesh_jni.cpp \
	render_pass.cpp \
	render_pass_jni.cpp \
	scene.cpp \
	scene_jni.cpp \
	scene_object.cpp \
	scene_object_jni.cpp \
	shader_data.cpp \
	shader_data_jni.cpp \
	uniform_block.cpp \
	vertex_bone_data.cpp \
	vertex_buffer.cpp \
	vertex_buffer_jni.cpp \


INCLUDES = \
		   -I../ \
		   -I../contrib \
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
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -g $< -o $@

-include $(OBJS:.o=.d)

.cpp.o:
	$(CXX) $(INCLUDES) -std=c++11 -c -g $< -o $(OBJSDIR)/$@

clean:
	rm -rf $(OBJS) $(OBJSDIR)

clobber: clean
	rm -f $(PROGRAM)

