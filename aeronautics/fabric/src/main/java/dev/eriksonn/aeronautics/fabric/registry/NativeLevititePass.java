package dev.eriksonn.aeronautics.fabric.registry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import foundry.veil.api.client.render.gpu.*;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.vertex.VertexArray;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.*;

/** Native cached four-by-four meshes, animated vertex noise and two real translucent motion ghosts. */
public final class NativeLevititePass implements AutoCloseable {
    public static final Identifier SHADER=Identifier.parse("aeronautics:levitite/levitite");
    public static final VertexFormat FORMAT=VertexFormat.builder(0).addAttribute("Position",GpuFormat.RGB32_FLOAT)
            .addAttribute("Color",GpuFormat.RGBA8_UNORM).addAttribute("UV0",GpuFormat.RG32_FLOAT)
            .addAttribute("UV2",GpuFormat.RG16_SINT).addAttribute("Normal",4,GpuFormat.RGB8_SNORM).build();
    public static final GpuUniformBuffer.Layout UNIFORMS=GpuUniformBuffer.layoutBuilder()
            .field("ModelViewMat",GpuUniformBuffer.ValueType.MAT4).field("ProjMat",GpuUniformBuffer.ValueType.MAT4)
            .field("InvProjMat",GpuUniformBuffer.ValueType.MAT4).field("SectionOffset",GpuUniformBuffer.ValueType.VEC3).field("time",GpuUniformBuffer.ValueType.FLOAT)
            .field("linearVelocity",GpuUniformBuffer.ValueType.VEC3).field("onSublevel",GpuUniformBuffer.ValueType.INT)
            .field("angularVelocity",GpuUniformBuffer.ValueType.VEC3).field("layerIndex",GpuUniformBuffer.ValueType.INT)
            .field("sublevelPosition",GpuUniformBuffer.ValueType.VEC3).field("gravityStrength",GpuUniformBuffer.ValueType.FLOAT)
            .field("currentOrientation",GpuUniformBuffer.ValueType.MAT3).field("materialTransitionSpeed",GpuUniformBuffer.ValueType.FLOAT)
            .field("SkyLightScale",GpuUniformBuffer.ValueType.FLOAT).field("DynamicShading",GpuUniformBuffer.ValueType.FLOAT)
            .field("materialMatrixSlow",GpuUniformBuffer.ValueType.MAT3).field("materialMatrixFast",GpuUniformBuffer.ValueType.MAT3)
            .field("ScreenSize",GpuUniformBuffer.ValueType.VEC2).field("LightDown",GpuUniformBuffer.ValueType.FLOAT).field("LightUp",GpuUniformBuffer.ValueType.FLOAT)
            .field("LightNorth",GpuUniformBuffer.ValueType.FLOAT).field("LightSouth",GpuUniformBuffer.ValueType.FLOAT)
            .field("LightWest",GpuUniformBuffer.ValueType.FLOAT).field("LightEast",GpuUniformBuffer.ValueType.FLOAT).build();
    public record Motion(Vector3fc linear,Vector3fc angular,Vector3fc position,Matrix3fc orientation,float gravity,
                         float transition,Matrix3fc slow,Matrix3fc fast,boolean sublevel,boolean ghosts) {
        public Motion {linear=new Vector3f(linear);angular=new Vector3f(angular);position=new Vector3f(position);orientation=new Matrix3f(orientation);slow=new Matrix3f(slow);fast=new Matrix3f(fast);}
    }
    public record Lighting(float down,float up,float north,float south,float west,float east,int sky,boolean dynamic) { }
    public record Request(Object key,List<LevititeQuadSubdivision.Vertex> vertices,Matrix4fc modelView,Vector3fc sectionOffset,Motion motion,Lighting lighting) {
        public Request {vertices=List.copyOf(vertices);modelView=new Matrix4f(modelView);sectionOffset=new Vector3f(sectionOffset);}
    }
    public record Texture(GpuTextureView view,GpuSampler sampler) { }
    private record State(GpuFormat color,GpuFormat depth,boolean ghost) { }
    private static final class Uploaded {
        final VertexArray mesh;long frame;
        Uploaded(VertexArray mesh,long frame){this.mesh=mesh;this.frame=frame;}
    }
    private final Map<State,RenderPipeline> pipelines=new HashMap<>();
    private final Map<Object,Uploaded> meshes=new IdentityHashMap<>();
    private ShaderManager owner;
    private GpuFramebuffer sceneDepth;
    private long frame;
    private RenderPipeline pipeline(State state) {
        return pipelines.computeIfAbsent(state,key->RenderPipeline.builder().withLocation(SHADER).withVertexShader(SHADER).withFragmentShader(SHADER)
                .withVertexBinding(0,FORMAT).withPrimitiveTopology(PrimitiveTopology.QUADS).withCull(!key.ghost)
                .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT),key.color,ColorTargetState.WRITE_ALL))
                .withDepthStencilState(new DepthStencilState(key.ghost?CompareOp.ALWAYS_PASS:CompareOp.GREATER_THAN_OR_EQUAL,!key.ghost))
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("LevititeUniforms",UniformType.UNIFORM_BUFFER)
                        .withUniform("Fog",UniformType.UNIFORM_BUFFER).withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER)
                        .withUniform("Sampler2",UniformType.COMBINED_IMAGE_SAMPLER).withUniform("Noise",UniformType.COMBINED_IMAGE_SAMPLER)
                        .withUniform("DiffuseDepthSampler",UniformType.COMBINED_IMAGE_SAMPLER).build()).build());
    }
    public Submission record(CommandEncoder encoder,ShaderManager shaders,GpuFramebuffer main,Matrix4fc projection,float time,
                             List<Request> requests,Texture atlas,Texture lightmap,Texture noise,GpuBufferSlice fog) {
        if(owner!=null&&owner!=shaders)close();owner=shaders;frame++;
        final var depthFormat=main.depth().texture().getFormat();
        if(sceneDepth==null||sceneDepth.depth().texture().getFormat()!=depthFormat) {
            if(sceneDepth!=null)sceneDepth.close();sceneDepth=GpuFramebuffer.builder("Levitite captured scene depth",main.width(),main.height()).depth(depthFormat).build(RenderSystem.getDevice());
        } else sceneDepth.resize(main.width(),main.height());
        main.copyDepthTo(encoder,sceneDepth);
        final var bindings=Map.of("Sampler0",new GpuShaderProgram.TextureBinding(atlas.view,atlas.sampler,null),
                "Sampler2",new GpuShaderProgram.TextureBinding(lightmap.view,lightmap.sampler,null),
                "Noise",new GpuShaderProgram.TextureBinding(noise.view,noise.sampler,null),
                "DiffuseDepthSampler",new GpuShaderProgram.TextureBinding(sceneDepth.depth().view(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.NEAREST,false),null));
        final var draws=new ArrayList<VertexArray.Draw>();final var leases=new ArrayList<GpuShaderProgram.Lease>();
        try {
            final Map<Request,VertexArray.Draw> selected=new IdentityHashMap<>();
            for(var request:requests) {
                var uploaded=meshes.get(request.key);
                if(uploaded==null) {
                    var mesh=VertexArray.create();
                    try(var cpu=new ByteBufferBuilder(Math.max(1024,request.vertices.size()*FORMAT.getVertexSize()))) {
                        var out=new BufferBuilder(cpu,PrimitiveTopology.QUADS,FORMAT);
                        for(var v:request.vertices)out.addVertex(v.x(),v.y(),v.z()).setColor(v.color()).setUv(v.u(),v.v()).setLight(v.light()).setNormal(v.nx(),v.ny(),v.nz());
                        mesh.upload(encoder,out.buildOrThrow(),VertexArray.DrawUsage.STATIC);
                    } catch(RuntimeException|Error failure){mesh.close();throw failure;}
                    uploaded=new Uploaded(mesh,frame);meshes.put(request.key,uploaded);
                }
                uploaded.frame=frame;var draw=uploaded.mesh.retain();draws.add(draw);selected.put(request,draw);
            }
            for(int layer:new int[]{0,1,-1})for(var request:requests) {
                if(layer!=0&&!request.motion.ghosts)continue;
                var state=pipeline(new State(main.color(0).texture().getFormat(),depthFormat,layer!=0));
                var program=shaders.getShader(SHADER,state,Runnable::run);leases.add(program.retain());
                var u=program.getUniformBlock("LevititeUniforms");if(u==null)u=program.createUniformBlock("LevititeUniforms",UNIFORMS);
                apply(u,request,projection,time,layer,main.width(),main.height());
                var captured=new HashMap<>(program.prepareUniformSnapshots(encoder,program.snapshotOwnedUniforms()));captured.put("Fog",fog);
                var draw=selected.get(request);draw.validatePipeline(state);
                try(var pass=main.beginPass(encoder)){program.bind(pass,captured,bindings);draw.draw(pass);}
            }
            var stale=meshes.values().iterator();while(stale.hasNext()){var mesh=stale.next();if(frame-mesh.frame>300){mesh.mesh.close();stale.remove();}}
            return new Submission(draws,leases);
        } catch(RuntimeException|Error failure){draws.forEach(VertexArray.Draw::close);leases.forEach(GpuShaderProgram.Lease::close);throw failure;}
    }
    private static void vector(GpuUniformBuffer u,String name,Vector3fc value){u.setVector(name,value.x(),value.y(),value.z());}
    private static void apply(GpuUniformBuffer u,Request r,Matrix4fc projection,float time,int layer,int width,int height) {
        var m=r.motion;var l=r.lighting;u.setMatrix("ModelViewMat",r.modelView);u.setMatrix("ProjMat",projection);u.setMatrix("InvProjMat",new Matrix4f(projection).invert());
        vector(u,"SectionOffset",r.sectionOffset);u.setFloat("time",time);vector(u,"linearVelocity",m.linear);vector(u,"angularVelocity",m.angular);vector(u,"sublevelPosition",m.position);
        u.setInt("onSublevel",m.sublevel?1:0);u.setInt("layerIndex",layer);u.setFloat("gravityStrength",m.gravity);u.setMatrix("currentOrientation",m.orientation);
        u.setFloat("materialTransitionSpeed",m.transition);u.setMatrix("materialMatrixSlow",m.slow);u.setMatrix("materialMatrixFast",m.fast);
        u.setFloat("SkyLightScale",l.sky/15f);u.setFloat("DynamicShading",l.dynamic?1:0);u.setVector("ScreenSize",width,height);
        u.setFloat("LightDown",l.down);u.setFloat("LightUp",l.up);u.setFloat("LightNorth",l.north);u.setFloat("LightSouth",l.south);u.setFloat("LightWest",l.west);u.setFloat("LightEast",l.east);
    }
    public record Submission(List<VertexArray.Draw> draws,List<GpuShaderProgram.Lease> leases) implements AutoCloseable {
        public Submission{draws=List.copyOf(draws);leases=List.copyOf(leases);}
        @Override public void close(){draws.forEach(VertexArray.Draw::close);leases.forEach(GpuShaderProgram.Lease::close);}
    }
    @Override public void close(){meshes.values().forEach(value->value.mesh.close());meshes.clear();if(sceneDepth!=null)sceneDepth.close();sceneDepth=null;
        if(owner!=null)pipelines.values().forEach(pipeline->owner.releaseShader(SHADER,pipeline));owner=null;pipelines.clear();}
}
