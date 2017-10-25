OS = linux
ARCH = x86_64

OBJSDIR = objs/$(OS)/$(ARCH)

SOURCES = \
	vk_bitmap_image.cpp \
	vk_cubemap_image.cpp \
	vk_Framebuffer.cpp \
	vk_imagebase.cpp \
	vk_render_target.cpp \
	vk_render_texture.cpp \
	vk_texture.cpp \
	vulkanCore.cpp \
	vulkanCore_jni.cpp \
	vulkan_index_buffer.cpp \
	vulkanInfoWrapper.cpp \
	vulkan_material.cpp \
	vulkan_render_data.cpp \
	vulkan_shader.cpp \
	vulkan_uniform_block.cpp \
	vulkan_vertex_buffer.cpp \
	vulkan_wrapper.cpp \


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


