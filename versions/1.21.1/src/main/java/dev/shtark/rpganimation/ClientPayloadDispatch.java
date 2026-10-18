package dev.shtark.rpganimation;

import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client registration installs the receiver without linking client classes on a server. */
public final class ClientPayloadDispatch {
    private ClientPayloadDispatch() {}

    public static Consumer<CustomPacketPayload> receiver = payload -> {
        throw new IllegalStateException("Client payload received without client initialization");
    };

    public static void handle(CustomPacketPayload payload, IPayloadContext context) {
        // The NeoForge registrar dispatches to the main thread by default.
        receiver.accept(payload);
    }
}
