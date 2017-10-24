#pragma once

#include <string>
#include <vector>
#include <map>

#ifdef __ANDROID__ // is this include really necessary?  not covered by gvr_gl.h??
#ifndef GL_ES_VERSION_3_0
#include "GLES3/gl3.h"
#endif
#endif
#include "glm/glm.hpp"
#include "gl/gl_program.h"
#include "util/gvr_gl.h"

#include "objects/vertex_buffer.h"


namespace gvr {
    class IndexBuffer;
    class Shader;

 /**
  * Interleaved vertex storage for OpenGL
  *
  * @see VertexBuffer
  */
    class GLVertexBuffer : public VertexBuffer
    {
    public:
        GLVertexBuffer(const char* layout_desc, int vertexCount);
        virtual ~GLVertexBuffer();

        virtual bool    updateGPU(Renderer*, IndexBuffer*f, Shader*);
        virtual void    bindToShader(Shader*, IndexBuffer*);

    protected:
        GLuint          mVBufferID;
        GLuint          mVArrayID;
        GLuint          mProgramID;
    };

} // end gvrf

