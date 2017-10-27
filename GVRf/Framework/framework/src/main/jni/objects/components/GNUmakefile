TOPDIR=../..
OS = linux
ARCH = x86_64

OBJSDIR = $(TOPDIR)/objs/$(OS)/$(ARCH)

SOURCES = \
	bone.cpp \
	bone_jni.cpp \
	box_collider.cpp \
	box_collider_jni.cpp \
	camera.cpp \
	camera_jni.cpp \
	camera_rig.cpp \
	camera_rig_jni.cpp \
	collider.cpp \
	collider_group.cpp \
	collider_group_jni.cpp \
	collider_jni.cpp \
	component_jni.cpp \
	custom_camera_jni.cpp \
	java_component_jni.cpp \
	mesh_collider.cpp \
	mesh_collider_jni.cpp \
	orthogonal_camera.cpp \
	orthogonal_camera_jni.cpp \
	perspective_camera.cpp \
	perspective_camera_jni.cpp \
	render_data.cpp \
	render_data_jni.cpp \
	render_target.cpp \
	render_target_jni.cpp \
	shadow_map.cpp \
	shadow_map_jni.cpp \
	sphere_collider.cpp \
	sphere_collider_jni.cpp \
	texture_capturer.cpp \
	texture_capturer_jni.cpp \
	transform.cpp \
	transform_jni.cpp \
	vertex_bone_data_jni.cpp \



INCLUDES = \
		   -I../../ \
		   -I../../contrib \
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


