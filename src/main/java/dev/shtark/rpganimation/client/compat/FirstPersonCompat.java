package dev.shtark.rpganimation.client.compat;

import dev.shtark.rpganimation.client.ClientConfig;
import dev.shtark.rpganimation.client.IntroController;
import dev.shtark.rpganimation.client.BedAnimations;
import dev.shtark.rpganimation.client.DropAnimations;
import dev.shtark.rpganimation.client.PickupAnimations;
import dev.shtark.rpganimation.client.ContextAnimations;
import dev.tr7zw.firstperson.api.ActivationHandler;
import dev.tr7zw.firstperson.api.FirstPersonAPI;

/** Loaded only on the client, after confirming the optional firstperson mod is present. */
public final class FirstPersonCompat {
    private static boolean registered;
    private static boolean querying;

    private FirstPersonCompat() {}

    public static void register() {
        if (registered) return;
        FirstPersonAPI.registerPlayerHandler((ActivationHandler) () -> presentation(true));
        registered = true;
    }
    /** FPM item visibility can re-enter this query while we inspect the held item. */
    public static boolean presentation(boolean includeBody) {
        if (querying) return false;
        querying = true;
        try {
            return ClientConfig.FIRST_PERSON_COMPATIBILITY.get()
                    && AnimationCompatibility.localPresentationAllowed()
                    && ((includeBody && (IntroController.active() || BedAnimations.localUpright(0) > 0))
                    || dev.shtark.rpganimation.client.BasicAnimations.localActive()
                    || DropAnimations.localActive() || PickupAnimations.localActive()
                    || dev.shtark.rpganimation.client.ActivityAnimations.typingHands()
                    || ContextAnimations.localActive() || ContextAnimations.fishingHands());
        } finally { querying = false; }
    }
}

