package org.valkyrienskies.clockwork.fabric.content.curiosities.particles;

import com.simibubi.create.Create;
import com.simibubi.create.content.curiosities.bell.BasicParticleData;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class PropellorStreamParticle extends SimpleAnimatedParticle {
    Vec3 motion;
    protected PropellorStreamParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite) {
        super(level, x, y ,z ,sprite, level.random.nextFloat()*.5f);
        this.quadSize *= 0.75f;
        this.lifetime = 20;
        hasPhysics = false;
        grabSprite(7);
        Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, Create.RANDOM, .5f);
        this.setPos(x+offset.x,y+offset.y,z+offset.z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        motion=new Vec3(dx,dy,dz);
        setAlpha(.25f);
    }
    @Nonnull
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
        } else {
            grabSprite((int) Mth.clamp((this.age / (float)this.lifetime)*8 + level.random.nextInt(1), 0, 7));
            xd = motion.x;
            yd = motion.y;
            zd = motion.z;
            double friction = 0.2*motion.lengthSqr();
            friction=Math.min(friction,0.5f);
            motion=motion.scale(1.0-friction);
            this.move(this.xd,this.yd,this.zd);
        }
    }
    private void grabSprite(int index) {
        setSprite(sprites.get(index, 8));
    }

    public static class Factory implements ParticleProvider<PropellorStreamParticleData> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet animSprite) {
            this.spriteSet = animSprite;
        }

        public Particle createParticle(PropellorStreamParticleData data, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new PropellorStreamParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}
