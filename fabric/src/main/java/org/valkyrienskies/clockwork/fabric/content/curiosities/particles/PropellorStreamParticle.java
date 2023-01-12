package org.valkyrienskies.clockwork.fabric.content.curiosities.particles;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.components.fan.IAirCurrentSource;
import com.simibubi.create.content.curiosities.bell.BasicParticleData;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class PropellorStreamParticle extends SimpleAnimatedParticle {

    private final IAirCurrentSource source;
    protected PropellorStreamParticle(ClientLevel world, IAirCurrentSource source, double x, double y, double z,
                                      SpriteSet sprite) {
        super(world, x, y ,z ,sprite, world.random.nextFloat()*.5f);
        this.source = source;
        this.quadSize *= 0.75f;
        this.lifetime = 20;
        hasPhysics = false;
        grabSprite(7);
        Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, Create.RANDOM, .5f);
        this.setPos(x+offset.x,y+offset.y,z+offset.z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        setAlpha(.25f);
    }
    @Nonnull
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (source == null || source.isSourceRemoved()) {
            dissipate();
            return;
        }

        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
        } else {
            if (source.getAirCurrent() == null || !source.getAirCurrent().bounds.inflate(.25f).contains(x, y, z)) {
                dissipate();
                return;
            }
            grabSprite((int) Mth.clamp((this.age / (float)this.lifetime)*8 + level.random.nextInt(1), 0, 7));
            Vec3 directionVec = Vec3.atLowerCornerOf(source.getAirCurrent().direction.getNormal());
            Vec3 motion = directionVec.scale(1 / 8f);
            if (!source.getAirCurrent().pushing)
                motion = motion.scale(-1);

            double distance = new Vec3(x, y, z).subtract(VecHelper.getCenterOf(source.getAirCurrentPos()))
                    .multiply(directionVec).length() - .5f;
            if (distance > source.getAirCurrent().maxDistance + 1 || distance < -.25f) {
                dissipate();
                return;
            }
            motion = motion.scale(source.getAirCurrent().maxDistance - (distance - 1f)).scale(.5f);
            grabSprite((int) Mth.clamp((distance / source.getAirCurrent().maxDistance) * 8 + level.random.nextInt(4),
                    0, 7));
            double friction = 0.2*motion.lengthSqr();
            friction=Math.min(friction,0.5f);
            motion=motion.scale(1.0-friction);
            xd = motion.x;
            yd = motion.y;
            zd = motion.z;

            this.move(this.xd,this.yd,this.zd);
        }
    }

    private void dissipate() {
        remove();
    }

    private void grabSprite(int index) {
        setSprite(sprites.get(index, 8));
    }

    public static class Factory implements ParticleProvider<PropellorStreamParticleData> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet animSprite) {
            this.spriteSet = animSprite;
        }

        public Particle createParticle(PropellorStreamParticleData data, ClientLevel worldIn, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            BlockEntity te = worldIn.getBlockEntity(new BlockPos(data.posX, data.posY, data.posZ));
            if (!(te instanceof IAirCurrentSource))
                te = null;
            return new PropellorStreamParticle(worldIn, (IAirCurrentSource) te, x, y, z, this.spriteSet);
        }
    }
}
