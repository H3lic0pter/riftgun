package dev.riftgun.client.render;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Optional Iris bridge. No Iris type is linked unless the mod is actually present. */
final class IrisPortalShaderEnvironment implements PortalShaderEnvironment {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";

    private final Object api;
    private final Method shaderPackInUse;
    private final Method renderingShadowPass;
    private volatile boolean failed;

    private IrisPortalShaderEnvironment(Object api, Method shaderPackInUse, Method renderingShadowPass) {
        this.api = api;
        this.shaderPackInUse = shaderPackInUse;
        this.renderingShadowPass = renderingShadowPass;
    }

    static PortalShaderEnvironment detect() {
        if (!ModList.get().isLoaded("iris")) return () -> State.INACTIVE;

        try {
            Class<?> apiType = Class.forName(IRIS_API, false,
                IrisPortalShaderEnvironment.class.getClassLoader());
            Object api = apiType.getMethod("getInstance").invoke(null);
            return new IrisPortalShaderEnvironment(api,
                apiType.getMethod("isShaderPackInUse"),
                apiType.getMethod("isRenderingShadowPass"));
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("Iris is present but its API could not be linked; using the safe portal fallback", error);
            return () -> State.COMPATIBILITY_FALLBACK;
        }
    }

    @Override
    public State snapshot() {
        if (failed) return State.COMPATIBILITY_FALLBACK;

        try {
            return new State((boolean) shaderPackInUse.invoke(api), false);
        } catch (ReflectiveOperationException | RuntimeException error) {
            failed = true;
            LOGGER.warn("Iris portal compatibility query failed; using the safe portal fallback", error);
            return State.COMPATIBILITY_FALLBACK;
        }
    }

    @Override
    public boolean shadowPass() {
        if (failed) return false;

        try {
            return (boolean) renderingShadowPass.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException error) {
            failed = true;
            LOGGER.warn("Iris shadow-pass query failed; using the safe portal fallback", error);
            return false;
        }
    }
}
