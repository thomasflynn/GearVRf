OS = linux
ARCH = x86_64

OBJSDIR = objs/$(OS)/$(ARCH)

SOURCES = \
	gl_bitmap_image.cpp \
	gl_cubemap_image.cpp \
	gl_image.cpp \
	gl_index_buffer.cpp \
	gl_material.cpp \
	gl_render_data.cpp \
	gl_render_image.cpp \
	gl_render_target.cpp \
	gl_render_texture.cpp \
	gl_shader.cpp \
	gl_uniform_block.cpp \
	gl_vertex_buffer.cpp \


INCLUDES = \
		   -I../ \
		   -I../contrib \
		   -I../util \
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
	$(CXX) $(INCLUDES) -std=c++11 -c -MMD -g -Wno-int-to-pointer-cast $< -o $@

-include $(OBJS:.o=.d)

.cpp.o:
	$(CXX) $(INCLUDES) -std=c++11 -c -g $< -o $(OBJSDIR)/$@

clean:
	rm -rf $(OBJS) $(OBJSDIR)

clobber: clean
	rm -f $(PROGRAM)


