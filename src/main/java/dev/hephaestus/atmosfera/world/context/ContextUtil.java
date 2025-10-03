package dev.hephaestus.atmosfera.world.context;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ContextUtil {
    public static final byte[][][][] OFFSETS = new byte[3][][][];

    public static ExecutorService buildDiscardingSingleDaemonThreadExecutor() {
        return new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            (runnable) -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public static final ExecutorService UPPER_HEMISPHERE_EXECUTOR = buildDiscardingSingleDaemonThreadExecutor();
    public static final ExecutorService LOWER_HEMISPHERE_EXECUTOR = buildDiscardingSingleDaemonThreadExecutor();

    private ContextUtil() {}

    static {
        Map<EnvironmentContext.Shape, Map<EnvironmentContext.Size, Collection<byte[]>>> offsets = new EnumMap<>(EnvironmentContext.Shape.class);

        for (EnvironmentContext.Size size : EnvironmentContext.Size.values()) {
            BlockPos origin = new BlockPos(0, 0, 0);

            byte radius = size.radius;
            for (byte x = 0; x <= radius + 1; ++x) {
                for (byte y = (byte) -radius; y <= 0; ++y) {
                    for (byte z = 0; z <= radius + 1; ++z) {
                        double distance = origin.getSquaredDistanceFromCenter(x, y, z);
                        if ((x + y + z) % 3 == 0 && distance <= (radius + 1) * (radius + 1)) {
                            offsets.computeIfAbsent(EnvironmentContext.Shape.LOWER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {x, y, z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.UPPER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {x, (byte) (-y + 1), z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.LOWER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {x, y, (byte) -z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.UPPER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {x, (byte) (-y + 1), (byte) -z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.LOWER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {(byte) -x, y, z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.UPPER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {(byte) -x, (byte) (-y + 1), z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.LOWER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {(byte) -x, y, (byte) -z}
                            );

                            offsets.computeIfAbsent(EnvironmentContext.Shape.UPPER_HEMISPHERE, key -> new EnumMap<>(EnvironmentContext.Size.class)).computeIfAbsent(size, key -> new HashSet<>()).add(
                                    new byte[] {(byte) -x, (byte) (-y + 1), (byte) -z}
                            );
                        }
                    }
                }
            }
        }

        for (EnvironmentContext.Shape shape : offsets.keySet()) {
            int shapeOrdinal = shape.ordinal();
            var shapes = offsets.get(shape);
            OFFSETS[shapeOrdinal] = new byte[shapes.size()][][];

            for (EnvironmentContext.Size size : shapes.keySet()) {
                int sizeOrdinal = size.ordinal();
                var sizes = shapes.get(size);
                OFFSETS[shapeOrdinal][sizeOrdinal] = new byte[sizes.size()][];

                int i = 0;
                for (byte[] bytes : sizes) {
                    OFFSETS[shapeOrdinal][sizeOrdinal][i++] = bytes;
                }
            }
        }
    }

    static void init() {}
}
