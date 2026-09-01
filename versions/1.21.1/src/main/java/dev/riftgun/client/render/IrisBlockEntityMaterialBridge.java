package dev.riftgun.client.render;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Isolated reflection bridge to the Iris block-entity material pipeline.
 *
 * <p>The wrapper and captured material state are the same mechanisms Iris uses around
 * vanilla block-entity renderers. Unknown Iris layouts fail closed: no geometry is drawn.
 */
final class IrisBlockEntityMaterialBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CAPTURED_STATE =
        "net.irisshaders.iris.uniforms.CapturedRenderingState";
    private static final String BLOCK_ENTITY_SHARD =
        "net.irisshaders.iris.layer.BlockEntityRenderStateShard";
    private static final String OUTER_WRAPPED_RENDER_TYPE =
        "net.irisshaders.iris.layer.OuterWrappedRenderType";
    private static final String BUFFER_SOURCE_WRAPPER =
        "net.irisshaders.iris.layer.BufferSourceWrapper";
    private static final IrisBlockEntityMaterialBridge INSTANCE = detect();

    private final Object capturedState;
    private final Method getCurrentBlockEntity;
    private final Method setCurrentBlockEntity;
    private final Object blockEntityShard;
    private final Method wrapExactlyOnce;
    private final Class<?> bufferSourceWrapper;
    private final Method getOriginalBufferSource;
    private final Map<RenderType, RenderType> wrappedTypes = new IdentityHashMap<>();
    private volatile RenderType cachedOriginal;
    private volatile RenderType cachedWrapped;
    private volatile boolean failed;

    private IrisBlockEntityMaterialBridge(Object capturedState, Method getCurrentBlockEntity,
                                          Method setCurrentBlockEntity, Object blockEntityShard,
                                          Method wrapExactlyOnce, Class<?> bufferSourceWrapper,
                                          Method getOriginalBufferSource, boolean failed) {
        this.capturedState = capturedState;
        this.getCurrentBlockEntity = getCurrentBlockEntity;
        this.setCurrentBlockEntity = setCurrentBlockEntity;
        this.blockEntityShard = blockEntityShard;
        this.wrapExactlyOnce = wrapExactlyOnce;
        this.bufferSourceWrapper = bufferSourceWrapper;
        this.getOriginalBufferSource = getOriginalBufferSource;
        this.failed = failed;
    }

    static IrisBlockEntityMaterialBridge instance() {
        return INSTANCE;
    }

    private static IrisBlockEntityMaterialBridge detect() {
        if (!ModList.get().isLoaded("iris")) return unavailable();
        try {
            ClassLoader loader = IrisBlockEntityMaterialBridge.class.getClassLoader();
            Class<?> capturedType = Class.forName(CAPTURED_STATE, false, loader);
            Class<?> shardType = Class.forName(BLOCK_ENTITY_SHARD, false, loader);
            Class<?> wrapperType = Class.forName(OUTER_WRAPPED_RENDER_TYPE, false, loader);
            Class<?> bufferWrapperType = Class.forName(BUFFER_SOURCE_WRAPPER, false, loader);
            Field capturedInstance = capturedType.getField("INSTANCE");
            Field shardInstance = shardType.getField("INSTANCE");
            Method wrapper = findWrapper(wrapperType);
            return new IrisBlockEntityMaterialBridge(
                capturedInstance.get(null),
                capturedType.getMethod("getCurrentRenderedBlockEntity"),
                capturedType.getMethod("setCurrentBlockEntity", int.class),
                shardInstance.get(null),
                wrapper,
                bufferWrapperType,
                bufferWrapperType.getMethod("getOriginal"),
                false);
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("Iris block-entity material bridge is incompatible; registered shader visuals are disabled",
                error);
            return unavailable();
        }
    }

    private static Method findWrapper(Class<?> wrapperType) throws NoSuchMethodException {
        for (Method method : wrapperType.getMethods()) {
            if (method.getName().equals("wrapExactlyOnce") && method.getParameterCount() == 3) {
                return method;
            }
        }
        throw new NoSuchMethodException(wrapperType.getName() + ".wrapExactlyOnce");
    }

    RenderType wrap(RenderType original) {
        if (failed) return null;
        if (cachedOriginal == original) return cachedWrapped;
        return wrapSlow(original);
    }

    private synchronized RenderType wrapSlow(RenderType original) {
        if (failed) return null;
        RenderType cached = wrappedTypes.get(original);
        if (cached != null) {
            cache(original, cached);
            return cached;
        }
        try {
            Object wrapped = wrapExactlyOnce.invoke(
                null, "riftgun:block_entity", original, blockEntityShard);
            if (!(wrapped instanceof RenderType renderType)) {
                throw new ReflectiveOperationException("Iris returned a non-RenderType wrapper");
            }
            wrappedTypes.put(original, renderType);
            cache(original, renderType);
            return renderType;
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail("Iris could not wrap a portal RenderType; registered shader visuals are disabled", error);
            return null;
        }
    }

    private void cache(RenderType original, RenderType wrapped) {
        cachedWrapped = wrapped;
        cachedOriginal = original;
    }

    MultiBufferSource originalBufferSource(MultiBufferSource source) {
        if (failed) return null;
        Object current = source;
        try {
            while (bufferSourceWrapper.isInstance(current)) {
                Object original = getOriginalBufferSource.invoke(current);
                if (original == current) {
                    throw new ReflectiveOperationException("Iris buffer wrapper returned itself");
                }
                current = original;
            }
            if (current instanceof MultiBufferSource buffers) return buffers;
            throw new ReflectiveOperationException("Iris returned a non-MultiBufferSource delegate");
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail("Iris entity buffer could not be unwrapped; registered shader visuals are disabled", error);
            return null;
        }
    }

    boolean renderWithMaterial(int materialId, Runnable draw) {
        if (failed) return false;
        int previous;
        try {
            previous = ((Number) getCurrentBlockEntity.invoke(capturedState)).intValue();
            setCurrentBlockEntity.invoke(capturedState, materialId);
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail("Iris material state could not be set; registered shader visuals are disabled", error);
            return false;
        }

        try {
            draw.run();
            return true;
        } finally {
            try {
                setCurrentBlockEntity.invoke(capturedState, previous);
            } catch (ReflectiveOperationException | RuntimeException error) {
                fail("Iris material state could not be restored; registered shader visuals are disabled", error);
            }
        }
    }

    private synchronized void fail(String message, Throwable error) {
        if (failed) return;
        failed = true;
        cachedOriginal = null;
        cachedWrapped = null;
        wrappedTypes.clear();
        LOGGER.warn(message, error);
    }

    private static IrisBlockEntityMaterialBridge unavailable() {
        return new IrisBlockEntityMaterialBridge(
            null, null, null, null, null, null, null, true);
    }
}
