package dev.identify.nftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

final class Recorder {
    private static final int FPS = 60;
    private static final byte[] END = new byte[0];

    private final String ffmpeg;
    private Process process;
    private Thread writer;
    private BlockingQueue<byte[]> queue;
    private long start;
    private long written;
    private volatile Throwable failure;

    Recorder(String ffmpeg) {
        this.ffmpeg = ffmpeg;
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post.class, event -> frame());
    }

    void start(Path file) {
        var target = Minecraft.getInstance().getMainRenderTarget();
        try {
            Files.createDirectories(file.getParent());
            process = new ProcessBuilder(List.of(ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                    "-f", "rawvideo", "-pix_fmt", "rgba", "-s", target.width + "x" + target.height, "-r", String.valueOf(FPS),
                    "-i", "-",
                    "-c:v", "h264_nvenc", "-preset", "p7", "-rc", "vbr", "-cq", "14", "-b:v", "0",
                    "-pix_fmt", "yuv420p", file.toString()))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        queue = new ArrayBlockingQueue<>(12);
        OutputStream out = process.getOutputStream();
        BlockingQueue<byte[]> frames = queue;
        writer = new Thread(() -> {
            try (out) {
                while (true) {
                    byte[] frame = frames.take();
                    if (frame == END) {
                        return;
                    }
                    out.write(frame);
                }
            } catch (Throwable e) {
                failure = e;
            }
        }, "identify-recorder");
        writer.start();
        start = System.nanoTime();
        written = 0;
    }

    private void frame() {
        if (process == null) {
            return;
        }
        long due = (System.nanoTime() - start) * FPS / 1_000_000_000L + 1;
        if (due <= written) {
            return;
        }
        long copies = due - written;
        written = due;
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            int[] pixels = image.getPixelsRGBA();
            byte[] bytes = new byte[pixels.length * 4];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(pixels);
            for (long i = 0; i < copies; i++) {
                queue.put(bytes);
            }
        } catch (Throwable e) {
            failure = e;
        }
    }

    void stop() {
        try {
            queue.put(END);
            writer.join();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            process = null;
        }
        if (failure != null) {
            System.out.println("[Identify showcase] recording failed " + failure);
            failure = null;
        }
    }
}
