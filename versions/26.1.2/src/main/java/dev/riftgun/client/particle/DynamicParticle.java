package dev.riftgun.client.particle;

import dev.riftgun.core.particle.ParticleDynamics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.util.LightCoordsUtil;

/** Engine-owned billboard particle driven by an immutable lifetime animation. */
public final class DynamicParticle extends SingleQuadParticle {
    private static final ParticleDynamics DEFAULT = new ParticleDynamics(
        20, 0.1F, 0.02F, 0xFFFFFFFF, 0x00FFFFFF, 0, 0.98F, 0, false, false);
    private ParticleDynamics dynamics = DEFAULT;

    private DynamicParticle(ClientLevel level, double x, double y, double z,
                            double vx, double vy, double vz, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        setParticleSpeed(vx, vy, vz);
        setSize(0.02F, 0.02F);
        configure(DEFAULT);
    }

    public void configure(ParticleDynamics next) {
        dynamics = java.util.Objects.requireNonNull(next);
        lifetime = next.lifetimeTicks();
        gravity = next.gravity();
        friction = next.drag();
        hasPhysics = next.collision();
        quadSize = Math.max(next.startSize(), next.endSize());
        updateColor();
    }

    @Override public void tick() {
        oRoll = roll;
        super.tick();
        if (!isAlive()) return;
        roll += dynamics.spin();
        updateColor();
    }

    private void updateColor() {
        int color = dynamics.color(age);
        setColor(((color >>> 16) & 255) / 255.0F, ((color >>> 8) & 255) / 255.0F, (color & 255) / 255.0F);
        alpha = ((color >>> 24) & 255) / 255.0F;
    }

    @Override public float getQuadSize(float partialTick) { return dynamics.size(age + partialTick); }

    @Override protected int getLightCoords(float partialTick) {
        return dynamics.fullBright() ? LightCoordsUtil.FULL_BRIGHT : super.getLightCoords(partialTick);
    }

    @Override public Layer getLayer() { return Layer.TRANSLUCENT; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            return new DynamicParticle(level, x, y, z, vx, vy, vz, sprites.get(random));
        }
    }
}
