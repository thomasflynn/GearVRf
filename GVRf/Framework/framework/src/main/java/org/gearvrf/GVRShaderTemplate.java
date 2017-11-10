/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gearvrf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gearvrf.utility.VrAppSettings;

import org.gearvrf.utility.Log;

/**
 * Generates a set of native vertex and fragment shaders from source code segments.
 * <p>
 * Each shader template keeps a set of named source code segments which are used
 * to compose the shaders. The code in the "FragmentTemplate" slot is the master
 * template for the fragment shader, the one attached as "VertexTemplate" is the
 * master for the vertex shader. Any name in the fragment template that starts
 * with "@Fragment" is replaced by the shader segment attached in that slot.
 * Similarly, names in the vertex template that start with "@Vertex" are
 * replaced with vertex shader segments. This permits multiple segments to be
 * combined into a single fragment or vertex shader. The segment names are also
 * #defined in the shader for conditional compilation.
 * <p>
 * To generate different variants from the same source we use #ifdef creatively.
 * Any shader variable name starting with a "HAS_" is assumed to be a #ifdef
 * that affects the generated code. These names typically correspond to
 * uniforms, textures or vertex buffer attributes. If these parameters are
 * defined by the material or mesh, that name will be #defined to 1 in the
 * shader. Otherwise, it will not be defined.
 * <p>
 * Each shader variant generated has a unique signature so that the same variant
 * will not be generated twice.
 * The shader also defines descriptors that define the
 * names and types of all the uniforms, textures and vertex attributes
 * used by the shader. For uniforms and attributes, each entry is a
 * float or integer type, an optional count and the name of the
 * uniform (e.g. "float3 diffuse_color, float specular_exponent, int is_enabled".
 * For textures, the descriptor contains the sampler type followed by the name:
 * (e.g. "sampler2D u_texture; samplerCube u_cubemap")
 * <p>
 * Multiple lights are supported by specifying light shader source code segments
 * in GVRLightBase. You can define different light implementations with
 * their own data structures and these will be included in the generated
 * fragment shader.
 * 
 * @see GVRPhongShader
 * @see GVRLightBase
 */
public class GVRShaderTemplate extends GVRShader
{
    private final static String TAG = "GVRShaderTemplate";

    protected class LightClass
    {
        public LightClass()
        {
            Count = 1;
            FragmentUniforms = "";
            VertexUniforms = null;
            VertexOutputs = null;
            FragmentShader = null;
            VertexShader = null;
        }
        public Integer Count;
        public String FragmentUniforms;
        public String VertexUniforms;
        public String VertexOutputs;
        public String VertexShader;
        public String FragmentShader;
    };

    /**
     * Construct a shader template for a shader using GLSL version 100.
     * To make a shader for another version use the other form of the constructor.
     *
     * @param uniformDescriptor string describing uniform names and types
     *                          e.g. "float4 diffuse_color, float4 specular_color, float specular_exponent"
     * @param textureDescriptor string describing texture names and types
     *                          e.g. "sampler2D diffuseTexture, sampler2D specularTexture"
     * @param vertexDescriptor  string describing vertex attributes and types
     *                          e.g. "float3 a_position, float2 a_texcoord"
     */
    public GVRShaderTemplate(String uniformDescriptor, String textureDescriptor, String vertexDescriptor)
    {
        super(uniformDescriptor, textureDescriptor, vertexDescriptor);
        mHasVariants = true;
    }

    /**
     * Construct a shader template
     *
     * @param uniformDescriptor string describing uniform names and types
     *                          e.g. "float4 diffuse_color, float4 specular_color, float specular_exponent"
     * @param textureDescriptor string describing texture names and types
     *                          e.g. "sampler2D diffuseTexture, sampler2D specularTexture"
     * @param vertexDescriptor  string describing vertex attributes and types
     *                          e.g. "float3 a_position, float2 a_texcoord"
     * @param glslVersion
     *            GLSL version (e.g. GLSLESVersion.V300)
     */
    public GVRShaderTemplate(String uniformDescriptor, String textureDescriptor, String vertexDescriptor, GLSLESVersion glslVersion)
    {
       super(uniformDescriptor, textureDescriptor, vertexDescriptor, glslVersion);
        mHasVariants = true;
    }

    /**
     * Attach a named shader segment.
     *
     * Shader segment names should start with either "Vertex" or "Fragment" to
     * designate which type of shader the code belongs with. Both vertex and
     * fragment shaders can have more than one code segment contribute to the
     * final shader.
     *
     * @param segmentName
     *            name associated with shader segment
     * @param shaderSource
     *            String with shader source code
     */
    protected void setSegment(String segmentName, String shaderSource)
    {
        super.setSegment(segmentName, shaderSource);
        Pattern pattern = Pattern.compile("HAS_([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(shaderSource);
        if (mShaderDefines == null) mShaderDefines = new HashSet<String>();
        int index = 0;
        while (((index = shaderSource.indexOf("HAS_", index)) >= 0) && matcher.find(index))
        {
            String match = matcher.group(1);
            mShaderDefines.add(match);
            index = matcher.end();
        }
    }

    /**
     * Create a unique signature for the lights used by this shader variant.
     * The signature will include the names of the light source classes and
     * how many times each is used in the shader.
     *
     * @param lightlist
     *            list of lights used with this shader
     * @return light string signature for shader
     */
    protected String generateLightSignature(GVRLightBase[] lightlist)
    {
        String sig = "";
        HashMap<Class<? extends GVRLightBase>, Integer> lightCount = new HashMap<Class<? extends GVRLightBase>, Integer>();

        if (lightlist != null)
        {
            for (GVRLightBase light : lightlist)
            {
                Integer n = lightCount.get(light.getClass());

                if (n == null)
                    lightCount.put(light.getClass(), 1);
                else
                    lightCount.put(light.getClass(), ++n);
            }
            for (Map.Entry<Class<? extends GVRLightBase>, Integer> entry : lightCount.entrySet())
                sig += "$" + entry.getKey().getSimpleName() + entry.getValue().toString();
        }
        return sig.trim();
    }

    /**
     * Generates the set of unique parameter names that make a particular
     * variant of the shader from the source template. Wherever the source
     * template contains "HAS_" followed by a name of a uniform, texture or
     * attribute used in the material or mesh, a "#define" for that name is
     * generated.
     *
     * @param definedNames
     *            set with defined names for this shader
     * @param vertexDesc
     *            String with vertex attributes, null to ignore them
     * @param material
     *            material used with this shader (may not be null)
     * @return shader signature string with names actually defined by the material and mesh
     */
    protected String generateVariantDefines(HashMap<String, Integer> definedNames, String vertexDesc, GVRShaderData material)
    {
        String signature = getClass().getSimpleName();

        for (String name : mShaderDefines)
        {
            if (definedNames.containsKey(name))
            {
                Integer value = definedNames.get(name);
                if (value != 0)
                {
                    signature += "$" + name;
                }
                continue;
            }
            if (material.hasUniform(name))
            {
                definedNames.put(name, 1);
                signature += "$" + name;
            }
            else if ((vertexDesc != null) && vertexDesc.contains(name))
            {
                definedNames.put(name, 1);
                if (!signature.contains(name))
                    signature += "$" + name;
            }
            else if (material.getTexture(name) != null)
            {
                definedNames.put(name, 1);
                signature += "$" + name;
                String attrname = material.getTexCoordAttr(name);
                if (attrname == null)
                {
                    attrname = "a_texcoord";
                }
                signature += "-" +"#"+ attrname+ "#";
            }
        }
        return signature;
    }

    protected void updateDescriptors(GVRShaderData material, String meshDesc,
                                     StringBuilder uniformDesc, StringBuilder textureDesc, StringBuilder vertexDesc)
    {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)[ \t]+([a-zA-Z0-9_]+)[^ ]*");
        Matcher matcher = pattern.matcher(mTextureDescriptor);
        String name;
        String type;

        while (matcher.find())
        {
            type = matcher.group(1);
            name = matcher.group(2);
            {
                textureDesc.append(type);
                textureDesc.append(' ');
                if (!material.hasTexture(name))
                {
                    textureDesc.append('!');
                }
                textureDesc.append(name);
                textureDesc.append(' ');
            }
        }
        matcher = pattern.matcher(mUniformDescriptor);
        while (matcher.find())
        {
            type = matcher.group(1);
            name = matcher.group(2);
            uniformDesc.append(type);
            uniformDesc.append(' ');
            if (!material.hasUniform(name))
            {
                uniformDesc.append('!');
            }
            uniformDesc.append(name);
            uniformDesc.append(' ');
        }
        if (meshDesc != null)
        {
            matcher = pattern.matcher(mVertexDescriptor);
            while (matcher.find())
            {
                type = matcher.group(1);
                name = matcher.group(2);
                vertexDesc.append(type);
                vertexDesc.append(' ');
                if (!meshDesc.contains(name))
                {
                    vertexDesc.append('!');
                }
                vertexDesc.append(name);
                vertexDesc.append(' ');

            }
        }
    }


    /**
     * Construct the source code for a GL shader based on the input defines. The
     * shader segments attached to slots that start with <type> are combined to
     * form the GL shader. #define statements are added to define compile-time
     * constants to control the code generated.
     *
     * @param type
     *            "Fragment" or "Vertex" indicating shader type.
     * @param definedNames
     *            set of names to define for this shader.
     * @param lightlist
     *            list of lights in the scene
     * @param lightClasses
     *            map of existing light classes used in scene
     * @param material
     *            GVRMaterial shader is being used with
     * @return GL shader code with parameters substituted.
     */
    private String generateShaderVariant(String type, HashMap<String, Integer> definedNames, GVRLightBase[] lightlist, Map<String, LightClass> lightClasses, GVRShaderData material)
    {
        String template = getSegment(type + "Template");
        StringBuilder shaderSource = new StringBuilder();

        if (template == null)
        {
            throw new IllegalArgumentException(type + "Template segment missing - cannot make shader");
        }
        String combinedSource = replaceTransforms(template);
        boolean useLights = (lightlist != null) && (lightlist.length > 0);
        String lightShaderSource = "";

        shaderSource.append("#version " + mGLSLVersion.toString() + "\n");
        if (definedNames.containsKey("LIGHTSOURCES") &&
            definedNames.get("LIGHTSOURCES") == 0)
        {
            useLights = false;
        }
        if (useLights)
        {
            if (type.equals("Vertex"))
            {
                lightShaderSource = generateLightVertexShader(lightlist, lightClasses);
            }
            else
            {
                lightShaderSource = generateLightFragmentShader(lightlist, lightClasses);
            }
            shaderSource.append("#define HAS_LIGHTSOURCES 1\n");
        }

        for (Map.Entry<String, String> entry : mShaderSegments.entrySet())
        {
            String key = entry.getKey();
            if (key.startsWith(type))
            {
                String segmentSource = entry.getValue();
                if (segmentSource == null)
                    segmentSource = "";
                if (!definedNames.containsKey(key) ||
                    (definedNames.get(key) != 0)) {
                    shaderSource.append("#define HAS_" + key + " 1;\n");
                }
                combinedSource = combinedSource.replace("@" + key, segmentSource);
            }
        }
        combinedSource = combinedSource.replace("@ShaderName", getClass().getSimpleName());
        combinedSource = combinedSource.replace("@LIGHTSOURCES", lightShaderSource);
        combinedSource = combinedSource.replace("@MATERIAL_UNIFORMS", material.makeShaderLayout());
        if(material != null && combinedSource.contains("Material_ubo"))
            material.useGpuBuffer(true);
        else
            material.useGpuBuffer(false);

        combinedSource = combinedSource.replace("@BONES_UNIFORMS", GVRShaderManager.makeLayout(sBonesDescriptor, "Bones_ubo", true));
        if (type.equals("Vertex"))
        {
            String texcoordSource = assignTexcoords(material);
            if (texcoordSource.length() > 0)
            {
                shaderSource.append("#define HAS_TEXCOORDS 1\n");
            }
            combinedSource = combinedSource.replace("@TEXCOORDS", texcoordSource);
        }
        for (Map.Entry<String, Integer> entry : definedNames.entrySet())
        {
            if (entry.getValue() != 0)
                shaderSource.append("#define HAS_" + entry.getKey() + " 1\n");
        }
        shaderSource.append(combinedSource);
        return shaderSource.toString();
    }

    /**
     * Generate the vertex shader assignments to copy texture
     * coordinates from the vertex array to shader variables.
     * @param mtl GVRMaterial being used with this shader.
     * @return shader code to assign texture coordinates
     */
    private String assignTexcoords(GVRShaderData mtl)
    {
        Set<String> texnames = mtl.getTextureNames();
        String shadercode = "";
        for (String name : texnames)
        {
            String texCoordAttr = mtl.getTexCoordAttr(name);
            String shaderVar = mtl.getTexCoordShaderVar(name);
            if (texCoordAttr != null)
            {
                shadercode += "    " + shaderVar + " = " + texCoordAttr + ";\n";
            }
        }
        return shadercode;
    }

    /**
     * Select the specific vertex and fragment shader to use.
     *
     * The shader template is used to generate the sources for the vertex and
     * fragment shader based on the vertex, material and light properties. This
     * function may compile the shader if it does not already exist.
     *
     * @param context
     *            GVRContext
     * @param rdata
     *            renderable entity with mesh and material
     * @param scene
     *            scene being rendered
     */
    public int bindShader(GVRContext context, IRenderable rdata, GVRScene scene, boolean isMultiview)
    {
        GVRMesh mesh = rdata.getMesh();
        GVRShaderData material = rdata.getMaterial();
        GVRLightBase[] lightlist = (scene != null) ? scene.getLightList() : null;
        HashMap<String, Integer> variantDefines = getRenderDefines(rdata, scene);

        if(isMultiview)
            variantDefines.put("MULTIVIEW", 1);
        else
            variantDefines.put("MULTIVIEW", 0);

        String meshDesc = mesh.getVertexBuffer().getDescriptor();
        String signature = generateVariantDefines(variantDefines, meshDesc, material);
        signature += generateLightSignature(lightlist);
        GVRMaterialShaderManager shaderManager = context.getMaterialShaderManager();
        int nativeShader = shaderManager.getShader(signature);

        synchronized (shaderManager)
        {
            if (nativeShader == 0)
            {
                Map<String, LightClass> lightClasses = scanLights(lightlist);

                String vertexShaderSource = generateShaderVariant("Vertex", variantDefines,
                                                                  lightlist, lightClasses, material);
                String fragmentShaderSource = generateShaderVariant("Fragment", variantDefines,
                                                                    lightlist, lightClasses, material);
                StringBuilder uniformDescriptor = new StringBuilder();
                StringBuilder textureDescriptor = new StringBuilder();
                StringBuilder vertexDescriptor = new StringBuilder();
                updateDescriptors(material, meshDesc, uniformDescriptor, textureDescriptor, vertexDescriptor);
                nativeShader = shaderManager.addShader(signature, uniformDescriptor.toString(),
                                                       textureDescriptor.toString(),
                                                       vertexDescriptor.toString(),
                                                       vertexShaderSource, fragmentShaderSource);
                bindCalcMatrixMethod(shaderManager, nativeShader);
                if (mWriteShadersToDisk)
                {
                    writeShader(context, "V-" + signature + ".glsl", vertexShaderSource);
                    writeShader(context, "F-" + signature + ".glsl", fragmentShaderSource);
                }
                Log.e(TAG, "SHADER: generated shader #%d %s", nativeShader, signature);
            }
            if (nativeShader > 0)
            {
                rdata.setShader(nativeShader, isMultiview);
            }
            return nativeShader;
        }
    }


    /**
     * Select the specific vertex and fragment shader to use with this material.
     * 
     * The shader template is used to generate the sources for the vertex and
     * fragment shader based on the material properties only.
     * It will ignore the mesh attributes and all lights.
     * 
     * @param context       GVRContext
     * @param material      material to use with the shader
     * @param meshDesc      string with vertex descriptor
     */
    public int bindShader(GVRContext context, GVRShaderData material, String meshDesc)
    {
        HashMap<String, Integer> variantDefines = new HashMap<String, Integer>();
        String signature = generateVariantDefines(variantDefines, meshDesc, material);
        GVRMaterialShaderManager shaderManager = context.getMaterialShaderManager();
        int nativeShader = shaderManager.getShader(signature);

        synchronized (shaderManager)
        {
            if (nativeShader == 0)
            {
                String vertexShaderSource =
                        generateShaderVariant("Vertex", variantDefines, null, null, material);
                String fragmentShaderSource =
                        generateShaderVariant("Fragment", variantDefines, null, null, material);
                StringBuilder uniformDescriptor = new StringBuilder();
                StringBuilder textureDescriptor = new StringBuilder();
                StringBuilder vertexDescriptor = new StringBuilder();

                updateDescriptors(material, meshDesc, uniformDescriptor, textureDescriptor, vertexDescriptor);
                nativeShader = shaderManager.addShader(signature, uniformDescriptor.toString(),
                                                       textureDescriptor.toString(), mVertexDescriptor,
                                                       vertexShaderSource, fragmentShaderSource);
                bindCalcMatrixMethod(shaderManager, nativeShader);
                if (mWriteShadersToDisk)
                {
                    writeShader(context, "V-" + signature + ".glsl", vertexShaderSource);
                    writeShader(context, "F-" + signature + ".glsl", fragmentShaderSource);
                }
                Log.e(TAG, "SHADER: generated shader #%d %s", nativeShader, signature);
            }
            return nativeShader;
        }
    }

    /**
     * Generate shader-specific defines from the rendering information.
     * You can override this function in your shader class to change which
     * variant is generated depending on the GVRRenderData settings.
     * 
     * The base implementation LIGHTSOURCES as 0 if lighting is not enabled by the render data,
     * and it defines SHADOWS as 1 if any light source enables shadow casting. 
     * 
     * @param renderable object being rendered by this shader
     * @param scene scene being rendered
     * @return list of symbols to be defined (value 1) or undefined (value 0) in the shader
     * 
     * @see GVRLightBase#setCastShadow(boolean) setCastShadow
     */
    public HashMap<String, Integer> getRenderDefines(IRenderable renderable, GVRScene scene) {
        HashMap<String, Integer> defines = new HashMap<String, Integer>();
        int castShadow = 0;
        GVRLightBase[] lights = (scene != null) ? scene.getLightList() : null;

        if (renderable.getGVRContext().getActivity().getAppSettings().isMultiviewSet())
        {
            defines.put("MULTIVIEW", 1);
        }
        if ((lights == null) || (lights.length == 0) || !renderable.isLightEnabled())
        {
            defines.put("LIGHTSOURCES", 0);
            return defines;
        }
        defines.put("LIGHTSOURCES", 1);
        for (GVRLightBase light : lights)
            if (light.getCastShadow())
                castShadow = 1;
        defines.put("SHADOWS", castShadow);
        return defines;
    }
    
     /**
     * Generates the shader code to compute fragment lighting for each light source.
     * The fragment shader defines a <LightPixel> function which computes the effect of all
     * light sources on the fragment color. This function calls the
     * <AddLight> function which integrates the light sources. This function
     * is defined in the fragment shader template.
     * 
     * @param lightlist
     *            list of lights in the scene
     * @return string with shader source code for fragment lighting
     */
    private String generateLightFragmentShader(GVRLightBase[] lightlist, Map<String, LightClass> lightClasses)
    {
        String lightFunction = "vec4 LightPixel(Surface s) {\n"
                + "   vec4 color = vec4(0.0, 0.0, 0.0, 0.0);\n"
                + "   vec4 c;\n"
                + "   float enable;\n"
                + "   Radiance r;\n";
        String lightDefs = "\n";
        String lightSources = "\n";
        Integer index = 0;

        for (GVRLightBase light : lightlist)
        {
            String lightClassName = light.getClass().getSimpleName();
            String uniformId = light.getLightID();
            String vertexId = "v" + uniformId;

            if (light.getFragmentShaderSource() != null)
            {
                String vertDesc = light.getVertexDescriptor();
                if (vertDesc != null)
                {
                    lightFunction += makeVertexCopy(vertDesc, vertexId, vertexId);
                    lightFunction += "   r = " + lightClassName + "(s, " + uniformId + ", " + vertexId;
                    lightFunction += ");\n";
                    lightDefs += makeVertexOutputs(vertDesc, vertexId, "in ");                    
                    lightSources += "Vertex" + lightClassName + " " + vertexId + ";\n";
                }
                else
                    lightFunction += "   r = " + lightClassName + "(s, " + uniformId + ");\n";
                lightFunction += "   enable = " + uniformId + ".enabled;\n";
                lightFunction += "   c = vec4(enable, enable, enable, 1) * AddLight(s, r);\n";
                lightFunction += "   color.xyz += c.xyz;\n";
                lightFunction += "   color.w = c.w;\n";
                lightSources += "\nuniform Uniform" + lightClassName + " " + uniformId + ";\n";
            }
            ++index;
        }
        for (Map.Entry<String, LightClass> entry : lightClasses.entrySet())
        {
            LightClass lclass = entry.getValue();
            
            if (lclass.FragmentShader == null)
                continue;
            lightDefs += "\n" + lclass.FragmentUniforms;            
            if (lclass.VertexOutputs != null)
                lightDefs += "\n" + lclass.VertexOutputs;
            lightDefs += lclass.FragmentShader;
        }
        lightFunction += "   return color; }\n";
        return lightDefs + lightSources + lightFunction;
    }

    /**
     * Generates the shader code to compute vertex lighting for each light source.
     * The vertex shader defines a <LightVertex> function which computes
     * vertex output information for each light
     * 
     * @param lightlist
     *            list of lights in the scene
     * @return string with shader source code for vertex lighting
     */
    private String generateLightVertexShader(GVRLightBase[] lightlist, Map<String, LightClass> lightClasses)
    {
        String lightSources = "";
        String lightDefs = "";
        String lightFunction = "void LightVertex(Vertex vertex) {\n";
        Integer index = 0;

        for (GVRLightBase light : lightlist)
        {
            String lightShader = light.getVertexShaderSource();
            String lightid = light.getLightID();

            if (lightid.isEmpty())
                continue;
            if (lightShader != null)
            {
                String vertexId = "v" + lightid;
                
                lightShader = lightShader.replace("@LIGHTOUT", vertexId);
                lightShader = lightShader.replace("@LIGHTIN", lightid);
                lightFunction += lightShader;
                lightDefs += makeVertexOutputs(light.getVertexDescriptor(), vertexId, "out ");
                lightSources += "\nuniform Uniform" + light.getClass().getSimpleName() + " " + lightid + ";\n";
            }
            ++index;
        }
        lightFunction += "}\n";
        for (Map.Entry<String, LightClass> entry : lightClasses.entrySet())
        {
            LightClass lclass = entry.getValue();
            
            if (lclass.VertexShader != null)
                lightDefs += lclass.FragmentUniforms;
        }
        return lightDefs + lightSources + lightFunction;
    }

    private Map<String, LightClass> scanLights(GVRLightBase[] lightlist)
    {
        Map<String, LightClass> lightClasses = new HashMap<String, LightClass>();

        if (lightlist == null)
            return lightClasses;
        for (GVRLightBase light : lightlist)
        {
            String lightClassName = light.getClass().getSimpleName();
            String lightid = light.getLightID();
            String lightShader = light.getFragmentShaderSource();
 
            if ((lightShader == null) || lightid.isEmpty())
                continue;
            LightClass lightClass = lightClasses.get(lightClassName);
            if (lightClass != null)
                ++lightClass.Count;
            else
            {
                lightClass = new LightClass();
                lightClass.FragmentShader = lightShader.replace("@LightType", lightClassName);
                lightClass.FragmentUniforms = makeShaderStruct(light.getUniformDescriptor(), "Uniform" + lightClassName, null);
                if (light.getVertexShaderSource() != null)
                {
                    lightClass.VertexShader = light.getVertexShaderSource().replace("@LightType", lightClassName);
                }
                if (light.getVertexDescriptor() != null)
                {
                    lightClass.VertexOutputs = makeShaderStruct(light.getVertexDescriptor(), "Vertex" + lightClassName, lightClass.VertexShader);
                }
                if (light.getUniformDescriptor() != null)
                {
                    lightClass.VertexUniforms = makeShaderStruct(light.getUniformDescriptor(), "Uniform" + lightClassName, lightClass.VertexShader);
                }
                lightClasses.put(lightClassName, lightClass);
            }
        }
        return lightClasses;
    }

    private String makeShaderStruct(String descriptor, String structName, String shaderSource)
    {
        Pattern pattern = Pattern.compile("[ ]*([a-zA-Z0-9_]+)[ ]+([A-Za-z0-9_]+)[,;:]*");
        Matcher matcher = pattern.matcher(descriptor);
        String structDesc = "struct " + structName + " {\n";
        while (matcher.find())
        {
            String name = matcher.group(2);
            String type = matcher.group(1);

            if ((shaderSource == null) ||
                shaderSource.contains(name))
                structDesc += "    " + type + " " + name + ";\n";
        }
        structDesc += "};\n";
        return structDesc;
    }

    private String makeVertexOutputs(String descriptor, String baseName, String prefix)
    {
        Pattern pattern = Pattern.compile("[ ]*([a-zA-Z0-9_]+)[ ]+([A-Za-z0-9_]+)[,;:]*");
        Matcher matcher = pattern.matcher(descriptor);
        String desc = "";
        while (matcher.find())
        {
            String name = matcher.group(2);
            String type = matcher.group(1);

            desc += prefix + type + " " + baseName + "_" + name + ";\n";
        }
        return desc;
    }

    private String makeVertexCopy(String descriptor, String inBase, String outBase)
    {
        Pattern pattern = Pattern.compile("[ ]*([a-zA-Z0-9_]+)[ ]+([A-Za-z0-9_]+)[,;:]*");
        Matcher matcher = pattern.matcher(descriptor);
        String desc = "";
        while (matcher.find())
        {
            String name = matcher.group(2);

            desc += "   " + outBase + "." + name + " = " +  inBase + "_" + name + ";\n";
        }
        return desc;
    }

    protected Set<String> mShaderDefines;
}
