package com.inspiration_mushroom.epic_third_person;

import com.inspiration_mushroom.epic_third_person.client.EpicFightLeawindCompatibility;
import com.inspiration_mushroom.epic_third_person.client.EpicThirdPersonClientCommands;
import com.inspiration_mushroom.epic_third_person.config.EpicThirdPersonClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("removal")
@Mod(EpicThirdPerson.MODID)
public class EpicThirdPerson {
    public static final String MODID = "epic_third_person";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public EpicThirdPerson() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EpicThirdPersonClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            EpicFightLeawindCompatibility.initialize();
            MinecraftForge.EVENT_BUS.addListener(EpicThirdPersonClientCommands::register);
        }
    }

}
