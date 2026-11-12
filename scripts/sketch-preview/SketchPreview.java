import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.PackResources;
import java.lang.reflect.Proxy;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.shaders.BlendMode;
import net.minecraft.client.renderer.ShaderInstance;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.awt.Graphics2D;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.valkyrienskies.clockwork.content.schematics.VdexSketchInk;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.valkyrienskies.clockwork.content.schematics.VdexPreviewRenderState;
import org.valkyrienskies.clockwork.content.schematics.VdexSketchRenderer;
public class SketchPreview {
    static ResourceProvider resources() {
        return id -> {
            Path path = Path.of("common/src/main/resources/assets", id.getNamespace(), id.getPath());
            if (!Files.exists(path)) return Optional.empty();
            var pack = (PackResources) Proxy.newProxyInstance(SketchPreview.class.getClassLoader(), new Class[] {
                PackResources.class
            }, (proxy, method, args) -> method.getName().equals("packId") ? "Clockwork preview" : false);
            return Optional.of(new Resource(pack, () -> path.toString().endsWith(".fsh") ? new ByteArrayInputStream(
                Files.readString(path).replace("#moj_import <clockwork_blueprint_reveal.glsl>",
                    Files.readString(SHADERS.resolve("../include/clockwork_blueprint_reveal.glsl"))).getBytes(StandardCharsets.UTF_8)) : Files.newInputStream(path)));
        };
    }
    static void checkVanillaBlendCache() throws Exception {
        for (String name : new String[]{"sketch", "ghost", "ink"}) {
            var format = name.equals("sketch") ? DefaultVertexFormat.POSITION_TEX : name.equals("ink") ? DefaultVertexFormat.POSITION_COLOR_TEX : DefaultVertexFormat.NEW_ENTITY;
            new BlendMode().apply();
            try (var shader = new ShaderInstance(resources(), "clockwork_blueprint_" + name, format)) {
                shader.apply();
                shader.clear();
                var vanilla = new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
                for (int frame = 0; frame < 3; frame++) {
                    vanilla.apply();
                    RenderSystem.blendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
                    vanilla.apply();
                    if (GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) != GL11.GL_ZERO || GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) != GL11.GL_ONE_MINUS_SRC_COLOR)
                        throw new AssertionError(name + " corrupted the vignette blend cache");
                }
            }
        }
        System.out.println("PASS: blueprint shaders preserve vanilla vignette blending across repeated frames.");
    }
    static BufferedImage inkImage(TextureTarget scene, VdexSketchInk.Drawing drawing, Matrix4f model, Matrix4f projection) throws Exception {
        return inkImage(scene, drawing, model, projection, 1, 0);
    }
    static BufferedImage inkImage(TextureTarget scene, VdexSketchInk.Drawing drawing, Matrix4f model, Matrix4f projection, float draw, float erase) throws Exception {
        var target = new TextureTarget(W, H, false, false);
        target.setClearColor(0, 0, 0, 0); target.clear(false); target.bindWrite(true);
        var strokes = VdexSketchInk.upload(drawing, model, projection, W / 3f, H / 3f);
        if (strokes != null) {
            try (var shader = new ShaderInstance(resources(), "clockwork_blueprint_ink", DefaultVertexFormat.POSITION_COLOR_TEX)) {
                RenderSystem.disableDepthTest(); RenderSystem.enableBlend();
                shader.MODEL_VIEW_MATRIX.set(new Matrix4f()); shader.PROJECTION_MATRIX.set(new Matrix4f());
                shader.safeGetUniform("RenderRect").set(-1f, 1f, 2f, -2f);
                shader.safeGetUniform("SketchSize").set((float)W, (float)H);
                shader.safeGetUniform("PixelScale").set(3f);
                shader.safeGetUniform("Reveal").set(draw, erase);
                shader.setSampler("Sampler0", scene.getDepthTextureId()); shader.apply();
                RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                strokes.bind(); strokes.draw(); VertexBuffer.unbind(); shader.clear();
            }
            strokes.close();
        }
        ByteBuffer pixels = MemoryUtil.memAlloc(W * H * 4);
        GL11.glReadPixels(0, 0, W, H, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        BufferedImage result = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = (y * W + x) * 4, a = pixels.get(i + 3) & 255;
            int r = a == 0 ? 0 : Math.min(255, (pixels.get(i) & 255) * 255 / a);
            int g = a == 0 ? 0 : Math.min(255, (pixels.get(i + 1) & 255) * 255 / a);
            int b = a == 0 ? 0 : Math.min(255, (pixels.get(i + 2) & 255) * 255 / a);
            result.setRGB(x, H - 1 - y, (a << 24) | (r << 16) | (g << 8) | b);
        }
        MemoryUtil.memFree(pixels); target.destroyBuffers(); RenderSystem.disableBlend(); return result;
    }
    static void checkInkOcclusion() throws Exception {
        var depth = new TextureTarget(W, H, true, false);
        depth.bindWrite(true); RenderSystem.depthMask(true); RenderSystem.clearDepth(.5);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, false);
        for (boolean hidden : new boolean[]{false, true}) {
            double z = hidden ? .2 : -.2;
            var drawing = new VdexSketchInk.Drawing(List.of(new VdexSketchInk.Stroke(List.of(new Vec3(-.7, 0, z), new Vec3(.7, 0, z)), 0x795699, 1f, 1f)), List.of());
            var picture = inkImage(depth, drawing, new Matrix4f(), new Matrix4f());
            int pixels = 0;
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if ((picture.getRGB(x, y) >>> 24) > 0) pixels++;
            if (hidden ? pixels != 0 : pixels < 100) throw new AssertionError("Ink depth occlusion failed: hidden=" + hidden + ", pixels=" + pixels);
        }
        depth.destroyBuffers(); RenderSystem.clearDepth(1);
        System.out.println("PASS: ink behind blocks is hidden; foreground ink remains visible.");
    }
    static final Path SHADERS=Path.of("common/src/main/resources/assets/minecraft/shaders/core");
    static final int W=576,H=432;
    static final Path OUTPUT=Path.of("build/sketch-preview");
    static int shader(int type,String source) throws Exception {
        source = source.replace("#moj_import <clockwork_blueprint_reveal.glsl>", Files.readString(SHADERS.resolve("../include/clockwork_blueprint_reveal.glsl")));
        int id=GL20.glCreateShader(type);
        GL20.glShaderSource(id,source);
        GL20.glCompileShader(id);
        if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0)throw new AssertionError(GL20.glGetShaderInfoLog(id));
        return id;
    }
    static int program(String vertex,String fragment)throws Exception {
        int p=GL20.glCreateProgram();
        GL20.glAttachShader(p,shader(GL20.GL_VERTEX_SHADER,vertex));
        GL20.glAttachShader(p,shader(GL20.GL_FRAGMENT_SHADER,fragment));
        GL20.glLinkProgram(p);
        if(GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==0)throw new AssertionError(GL20.glGetProgramInfoLog(p));
        return p;
    }
    static void matrix(int p,String name,Matrix4f m) {
        float[] v=new float[16];
        m.get(v);
        GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(p,name),false,v);
    }
    static List<Float> vertices=new ArrayList<>();
    static void vertex(float[] p,float[] n,float tone) {
        for(float v:p)vertices.add(v);
        vertices.add(tone);
        vertices.add(tone);
        vertices.add(tone);
        vertices.add(1f);
        vertices.add(-1f);
        vertices.add(-1f);
        for(float v:n)vertices.add(v);
    }
    static void box(float x,float y,float z,float w,float h,float d,float tone) {
        float[][] p= {
            {
                x,y,z
            }, {
                x+w,y,z
            }, {
                x+w,y+h,z
            }, {
                x,y+h,z
            }, {
                x,y,z+d
            }, {
                x+w,y,z+d
            }, {
                x+w,y+h,z+d
            }, {
                x,y+h,z+d
            }
        };
        int[][] faces= {
            {
                0,3,2,1
            }, {
                4,5,6,7
            }, {
                0,4,7,3
            }, {
                1,2,6,5
            }, {
                3,7,6,2
            }, {
                0,1,5,4
            }
        };
        float[][] normals= {
            {
                0,0,-1
            }, {
                0,0,1
            }, {
                -1,0,0
            }, {
                1,0,0
            }, {
                0,1,0
            }, {
                0,-1,0
            }
        };
        for(int f=0;f<6;f++)for(int i:new int[] {
            0,1,2,0,2,3
        })vertex(p[faces[f][i]],normals[f],tone);
    }
    static void attribute(int p,String name,int n,int stride,int offset) {
        int l=GL20.glGetAttribLocation(p,name);
        if(l<0)return;
        GL20.glEnableVertexAttribArray(l);
        GL20.glVertexAttribPointer(l,n,GL11.GL_FLOAT,false,stride,offset);
    }
    static int image(TextureTarget target,String name)throws Exception {
        target.bindWrite(true);
        ByteBuffer pixels=MemoryUtil.memAlloc(W*H*4);
        GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        int marks=0;
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            int i=(y*W+x)*4;
            float a=(pixels.get(i+3)&255)/255f;
            int r=Math.round((pixels.get(i)&255)*a+227*(1-a));
            int g=Math.round((pixels.get(i+1)&255)*a+245*(1-a));
            int b=Math.round((pixels.get(i+2)&255)*a+250*(1-a));
            img.setRGB(x,H-y-1,(r<<16)|(g<<8)|b);
            if(a>.1)marks++;
        }
        MemoryUtil.memFree(pixels);
        if(marks<500 && !name.startsWith("animation/"))throw new AssertionError("Blank sketch");
        ImageIO.write(img,"png",OUTPUT.resolve(name+".png").toFile());
        return marks;
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);
        if(!GLFW.glfwInit())throw new AssertionError("GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        long window=GLFW.glfwCreateWindow(W,H,"Sketch shader verification",0,0);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        RenderSystem.initRenderThread();
        RenderSystem.beginInitialization();
        int geometry=program(Files.readString(SHADERS.resolve("clockwork_blueprint_geometry.vsh")),Files.readString(SHADERS.resolve("clockwork_blueprint_geometry.fsh")));
        var scene=new TextureTarget(W,H,true,false);
        scene.setClearColor(0,0,0,0);
        scene.clear(false);
        scene.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        String fixture=args.length==0?"dense":args[0];
        if(fixture.equals("mechanism")) {
            box(-3,0,-1,6,.4f,2,.55f);
            box(-2,.4f,-.7f,2,1.5f,1.4f,.72f);
            box(.3f,.4f,-.6f,2,1,1.2f,.4f);
            box(-1,1.9f,-.4f,.45f,1.4f,.8f,.55f);
            box(-2.8f,-.7f,-1.4f,1,.7f,.7f,.3f);
            box(1.7f,-.7f,-1.4f,1,.7f,.7f,.3f);
            box(-2.8f,-.7f,.7f,1,.7f,.7f,.3f);
            box(1.7f,-.7f,.7f,1,.7f,.7f,.3f);
            for(int i=0;i<9;i++)box(.4f+i*.2f,1.4f,-.6f,.06f,.04f,1.2f,.7f);
            for(int i=0;i<4;i++)box(-2+i*.48f,.5f,-.72f,.035f,1.1f,.03f,.28f);
        } else if (fixture.equals("connections")) {
            for (int row = 0; row < 3; row++) {
                float z = -2.2f + row * 2.1f;
                box(-3.5f, 0, z, .7f, 2.2f, .7f, .62f);
                box(2.8f, 0, z, .7f, 2.2f, .7f, .62f);
            }
            box(-.4f, -.25f, -.2f, .8f, 1.55f, .8f, .52f);
        } else {
            box(-3.5f,-.35f,-2.2f,7,.35f,4.4f,.52f);
            for(int x=0;x<7;x++)for(int z=0;z<4;z++) {
                float height=.5f+((x*3+z*5)%5)*.27f;
                box(-3.3f+x*.94f,0,-2+z*.98f,.79f,height,.80f,.38f+((x+z*2)%5)*.085f);
                for(int rib=0;rib<4;rib++)box(-3.25f+x*.94f+rib*.18f,height,-1.96f+z*.98f,.035f,.045f,.70f,.70f);
            }
            box(-2.6f,2.0f,-1.5f,.12f,1.2f,3.0f,.60f);
            box(1.8f,2.0f,-1.5f,.12f,1.2f,3.0f,.60f);
            box(-2.6f,3.1f,-1.5f,4.52f,.12f,.12f,.45f);
            box(-2.6f,3.1f,1.38f,4.52f,.12f,.12f,.45f);
        }
        float[] mesh=new float[vertices.size()];
        for(int i=0;i<mesh.length;i++)mesh[i]=vertices.get(i);
        int vao=GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo=GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,mesh,GL15.GL_STATIC_DRAW);
        GL20.glUseProgram(geometry);
        attribute(geometry,"Position",3,48,0);
        attribute(geometry,"Color",4,48,12);
        attribute(geometry,"UV0",2,48,28);
        attribute(geometry,"Normal",3,48,36);
        matrix(geometry,"ModelViewMat",new Matrix4f().rotateX(.45f).rotateY(-.65f).translate(0,-1,0));
        matrix(geometry,"ProjMat",new Matrix4f().setOrtho(-5.0f,5.0f,-3.75f,3.75f,-20,20));
        GL11.glDrawArrays(GL11.GL_TRIANGLES,0,mesh.length/12);
        var renderer=new VdexSketchRenderer();
        var targetField=VdexSketchRenderer.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(renderer,scene);
        var measure=VdexSketchRenderer.class.getDeclaredMethod("measureTones",TextureTarget.class);
        measure.setAccessible(true);
        try(var guard=new VdexPreviewRenderState()) {
            measure.invoke(renderer,scene);
        }
        ByteBuffer packed=MemoryUtil.memAlloc(W*H*4);
        scene.bindWrite(true);
        GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,packed);
        BufferedImage input=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            int i=(y*W+x)*4;
            input.setRGB(x,H-1-y,((packed.get(i+3)&255)<<24)|((packed.get(i)&255)<<16)|((packed.get(i+1)&255)<<8)|(packed.get(i+2)&255));
        }
        ImageIO.write(input,"png",OUTPUT.resolve(fixture+"-geometry.png").toFile());
        MemoryUtil.memFree(packed);
        var rangeField=VdexSketchRenderer.class.getDeclaredField("toneRange");
        rangeField.setAccessible(true);
        Vector2f range=(Vector2f)rangeField.get(renderer);
        Files.writeString(OUTPUT.resolve(fixture+"-tones.json"),"["+range.x+","+range.y+"]");
        var inkStrokes = new ArrayList<VdexSketchInk.Stroke>();
        var inkSymbols = new ArrayList<VdexSketchInk.Symbol>();
        if (fixture.equals("connections")) {
            VdexSketchInk.Kind[] kinds = {VdexSketchInk.Kind.SPRING, VdexSketchInk.Kind.ROPE, VdexSketchInk.Kind.THINKABLE};
            for (int row = 0; row < 3; row++) {
                double z = -1.85 + row * 2.1;
                var drawing = VdexSketchInk.connection(kinds[row], new Vec3(-2.8, 1.8, z), new Vec3(2.8, 1.8, z), row == 0 ? 5.6 : 6.8);
                inkStrokes.addAll(drawing.getStrokes()); inkSymbols.addAll(drawing.getSymbols());
            }
        }
        var drawing = new VdexSketchInk.Drawing(inkStrokes, inkSymbols);
        var inkModel = new Matrix4f().rotateX(.45f).rotateY(-.65f).translate(0,-1,0);
        var inkProjection = new Matrix4f().setOrtho(-5,5,-3.75f,3.75f,-20,20);
        var overlay = inkImage(scene, drawing, inkModel, inkProjection);
        ImageIO.write(overlay,"png",OUTPUT.resolve(fixture+"-ink.png").toFile());
        int quad=GL30.glGenVertexArrays();
        GL30.glBindVertexArray(quad);
        int quadBuffer=GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,quadBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,new float[] {
            -1,-1,0,0,0,1,-1,0,1,0,1,1,0,1,1,-1,-1,0,0,0,1,1,0,1,1,-1,1,0,0,1
        },GL15.GL_STATIC_DRAW);
        var output=new TextureTarget(W,H,false,false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        for(String name:new String[] {
            "current","clear","light"
        }) {
            int sketch=program(Files.readString(SHADERS.resolve("clockwork_blueprint_sketch.vsh")),Files.readString(SHADERS.resolve("clockwork_blueprint_sketch.fsh")));
            output.bindWrite(true);
            GL20.glUseProgram(sketch);
            attribute(sketch,"Position",3,20,0);
            attribute(sketch,"UV0",2,20,12);
            matrix(sketch,"ModelViewMat",new Matrix4f());
            matrix(sketch,"ProjMat",new Matrix4f());
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,scene.getColorTextureId());
            GL20.glUniform1i(GL20.glGetUniformLocation(sketch,"Sampler0"),0);
            GL20.glUniform2f(GL20.glGetUniformLocation(sketch,"SketchSize"),W,H);
            GL20.glUniform1f(GL20.glGetUniformLocation(sketch,"PixelScale"),3);
            GL20.glUniform2f(GL20.glGetUniformLocation(sketch,"ToneRange"),range.x,range.y);
            float[] style=switch(name) {
                case "clear"->new float[] {
                    .86f,.25f,.75f,1.20f
                };
                case "light"->new float[] {
                    .42f,.38f,.25f,.65f
                };
                default->new float[] {
                    1,1,1,1
                };
            };
            GL20.glUniform4fv(GL20.glGetUniformLocation(sketch,"StrokeStyle"),style);
            GL20.glUniform2f(GL20.glGetUniformLocation(sketch,"Reveal"),1,0);
            GL11.glDrawArrays(GL11.GL_TRIANGLES,0,6);
            image(output,fixture+"-"+name);
            var picture = ImageIO.read(OUTPUT.resolve(fixture+"-"+name+".png").toFile());
            Graphics2D composite = picture.createGraphics(); composite.drawImage(overlay, 0, 0, null); composite.dispose();
            ImageIO.write(picture, "png", OUTPUT.resolve(fixture+"-"+name+".png").toFile());
            if (fixture.equals("connections") && name.equals("clear")) {
                Files.createDirectories(OUTPUT.resolve("animation"));
                int full = -1, middle = -1;
                for (int frame = 0; frame <= 90; frame++) {
                    float seconds = frame / 30f;
                    float reveal = Math.min(1, seconds / 1.45f), erase = Math.max(0, Math.min(1, (seconds - 2.0f) / .65f));
                    var animatedInk = inkImage(scene, drawing, inkModel, inkProjection, reveal, erase);
                    int inkMarks = 0;
                    for (int iy = 0; iy < H; iy++) for (int ix = 0; ix < W; ix++) if ((animatedInk.getRGB(ix, iy) >>> 24) > 0) inkMarks++;
                    if ((frame == 0 || frame == 90) && inkMarks != 0) throw new AssertionError("Ink reveal endpoint must be empty");
                    if (frame == 50 && inkMarks < 100) throw new AssertionError("Ink reveal did not complete");
                    output.bindWrite(true); RenderSystem.disableDepthTest(); RenderSystem.disableBlend();
                    GL30.glBindVertexArray(quad); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadBuffer);
                    GL20.glUseProgram(sketch); attribute(sketch,"Position",3,20,0); attribute(sketch,"UV0",2,20,12);
                    GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glBindTexture(GL11.GL_TEXTURE_2D,scene.getColorTextureId());
                    GL20.glUniform2f(GL20.glGetUniformLocation(sketch,"Reveal"),reveal,erase);
                    GL11.glDrawArrays(GL11.GL_TRIANGLES,0,6);
                    String file = "animation/" + String.format("%03d", frame);
                    int marks = image(output, file);
                    if ((frame == 0 || frame == 90) && marks != 0) throw new AssertionError("Sketch reveal endpoint must be empty");
                    if (frame == 22) middle = marks;
                    if (frame == 50) full = marks;
                    var animated = ImageIO.read(OUTPUT.resolve(file+".png").toFile());
                    var paint = animated.createGraphics(); paint.drawImage(animatedInk,0,0,null); paint.dispose();
                    ImageIO.write(animated,"png",OUTPUT.resolve(file+".png").toFile());
                }
                if (middle <= 0 || full <= middle) throw new AssertionError("Sketch reveal must grow to the full image");
                System.out.println("PASS: sketch and connection ink draw in and erase to an empty page.");
            }
            GL20.glDeleteProgram(sketch);
        }
        int webProgram = program(
            Files.readString(SHADERS.resolve("clockwork_blueprint_sketch.vsh")).replace("#version 150", "#version 300 es\nprecision highp float;"),
            Files.readString(SHADERS.resolve("clockwork_blueprint_sketch.fsh")).replace("#version 150", "#version 300 es\nprecision highp float;"));
        GL20.glDeleteProgram(webProgram);
        checkInkOcclusion();
        checkVanillaBlendCache();
        WorldSketchCheck.check(mesh, drawing, inkModel, inkProjection, fixture);
        if(GL11.glGetError()!=0)throw new AssertionError("OpenGL error");
        renderer.close();
        output.destroyBuffers();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        System.out.println("Rendered sketch variants successfully.");
    }
}
