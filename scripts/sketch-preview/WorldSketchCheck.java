import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.valkyrienskies.clockwork.ClockworkShaders;
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintData;
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintPreview;
import org.valkyrienskies.clockwork.content.schematics.VdexSketchInk;
import org.valkyrienskies.clockwork.content.schematics.VdexWorldSketchRenderer;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class WorldSketchCheck {
    static void field(Object object, Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
    static Map<String, String> state() {
        var values = new LinkedHashMap<String, String>();
        for (int key : new int[]{GL30.GL_DRAW_FRAMEBUFFER_BINDING, GL30.GL_READ_FRAMEBUFFER_BINDING, GL11.GL_DEPTH_FUNC,
                GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_ALPHA,
                GL20.GL_BLEND_EQUATION_RGB, GL20.GL_BLEND_EQUATION_ALPHA}) values.put("gl:"+key, ""+GL11.glGetInteger(key));
        for (int key : new int[]{GL11.GL_DEPTH_TEST, GL11.GL_CULL_FACE, GL11.GL_BLEND, GL11.GL_SCISSOR_TEST}) values.put("enabled:"+key,""+GL11.glIsEnabled(key));
        values.put("depthMask",""+GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport); values.put("viewport",Arrays.toString(viewport));
        int active = GlStateManager._getActiveTexture(); values.put("active",""+active);
        for (int unit = 0; unit < 3; unit++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
            values.put("binding:"+unit,""+GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            values.put("sampler:"+unit,""+RenderSystem.getShaderTexture(unit));
        }
        RenderSystem.activeTexture(active);
        return values;
    }
    static byte[] pixels(int width, int height) {
        var buffer = MemoryUtil.memAlloc(width * height * 4);
        GL11.glReadPixels(0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,buffer);
        byte[] data = new byte[buffer.remaining()]; buffer.get(data); MemoryUtil.memFree(buffer); return data;
    }
    static void check(float[] vertices, VdexSketchInk.Drawing drawing, Matrix4f model, Matrix4f projection, String fixture) throws Exception {
        int width = SketchPreview.W, height = SketchPreview.H;
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe)unsafeField.get(null);
        var previous = Minecraft.getInstance();
        var mc = (Minecraft)unsafe.allocateInstance(Minecraft.class);
        field(null,Minecraft.class,"instance",mc);
        var window = unsafe.allocateInstance(com.mojang.blaze3d.platform.Window.class);
        field(window,window.getClass(),"guiScale",3.0); field(mc,Minecraft.class,"window",window);
        var world = new TextureTarget(width,height,true,false);
        field(mc,Minecraft.class,"mainRenderTarget",world);
        var textures = (TextureManager)unsafe.allocateInstance(TextureManager.class);
        var atlas = new DynamicTexture(16,16,false);
        field(textures,TextureManager.class,"byPath",new HashMap<>(Map.of(TextureAtlas.LOCATION_BLOCKS,atlas)));
        field(mc,Minecraft.class,"textureManager",textures);
        var geometry = new ShaderInstance(SketchPreview.resources(),"clockwork_blueprint_geometry",DefaultVertexFormat.NEW_ENTITY);
        var sketch = new ShaderInstance(SketchPreview.resources(),"clockwork_blueprint_sketch",DefaultVertexFormat.POSITION_TEX);
        var ink = new ShaderInstance(SketchPreview.resources(),"clockwork_blueprint_ink",DefaultVertexFormat.POSITION_COLOR_TEX);
        field(null,ClockworkShaders.class,"blueprintGeometry",geometry);
        field(null,ClockworkShaders.class,"blueprintSketch",sketch);
        field(null,ClockworkShaders.class,"blueprintInk",ink);
        var builder = new BufferBuilder(262144);
        builder.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.NEW_ENTITY);
        for (int i=0;i<vertices.length;i+=12) builder.vertex(vertices[i],vertices[i+1],vertices[i+2])
            .color(vertices[i+3],vertices[i+4],vertices[i+5],vertices[i+6]).uv(vertices[i+7],vertices[i+8])
            .overlayCoords(0).uv2(0).normal(vertices[i+9],vertices[i+10],vertices[i+11]).endVertex();
        var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC); buffer.bind(); buffer.upload(builder.end()); VertexBuffer.unbind();
        var data = (VdexBlueprintData)unsafe.allocateInstance(VdexBlueprintData.class);
        var mesh = (VdexBlueprintPreview.Mesh)unsafe.allocateInstance(VdexBlueprintPreview.Mesh.class);
        field(mesh,mesh.getClass(),"data",data); field(mesh,mesh.getClass(),"buffer",buffer);
        field(mesh,mesh.getClass(),"ink$delegate",kotlin.LazyKt.lazyOf(drawing));
        var renderer = new VdexWorldSketchRenderer();
        for (int pass=0;pass<3;pass++) {
            world.setClearColor(.22f,.34f,.24f,1); RenderSystem.depthMask(true); RenderSystem.clearDepth(1);
            world.clear(false); world.bindWrite(true);
            RenderSystem.enableScissor(0,height/2,width,height/2); RenderSystem.clearColor(.43f,.64f,.82f,1); RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT,false);
            RenderSystem.disableScissor();
            if (pass>0) {
                RenderSystem.enableScissor(0,0,width/2,height); RenderSystem.clearDepth(.1); RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT,false); RenderSystem.disableScissor();
            }
            byte[] beforePixels = pixels(width,height);
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LESS); RenderSystem.depthMask(false);
            RenderSystem.activeTexture(GL13.GL_TEXTURE2);
            var before = state(); renderer.render(mesh,model,projection);
            var after = state();
            if (!before.equals(after)) throw new AssertionError("World sketch leaked GL state: "+before+" -> "+after);
            byte[] afterPixels = pixels(width,height);
            int changed = 0, blocked = 0;
            var image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int i=(y*width+x)*4;
                boolean different = afterPixels[i]!=beforePixels[i] || afterPixels[i+1]!=beforePixels[i+1] || afterPixels[i+2]!=beforePixels[i+2];
                if(different) { changed++; if(pass>0&&x<width/2)blocked++; }
                image.setRGB(x,height-y-1,((afterPixels[i]&255)<<16)|((afterPixels[i+1]&255)<<8)|(afterPixels[i+2]&255));
            }
            if(changed<500 || blocked!=0)throw new AssertionError("World sketch occlusion: changed="+changed+", behind terrain="+blocked);
            float[] depth = new float[width*height]; GL11.glReadPixels(0,0,width,height,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,depth);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(Math.abs(depth[y*width+x]-(pass>0&&x<width/2?.1:1))>.00001)
                throw new AssertionError("World sketch modified the world's depth buffer");
            ImageIO.write(image,"png",SketchPreview.OUTPUT.resolve(fixture+(pass==0?"-world.png":"-world-occluded.png")).toFile());
        }
        renderer.close(); buffer.close(); geometry.close(); sketch.close(); ink.close(); atlas.close(); world.destroyBuffers();
        field(null,Minecraft.class,"instance",previous);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0); RenderSystem.depthMask(true); RenderSystem.clearDepth(1);
        System.out.println("PASS: production world sketch preserves GL state and world depth; terrain occludes blocks and connection ink, including cached frames.");
    }
}
