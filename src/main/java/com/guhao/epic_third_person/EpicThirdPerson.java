package com.guhao.epic_third_person;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("removal")
@Mod(EpicThirdPerson.MODID)
public class EpicThirdPerson {
    public static final String MODID = "epic_third_person";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public EpicThirdPerson() {
        MinecraftForge.EVENT_BUS.register(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            EpicFightLeawindCompatibility.initialize();
        }
    }

}
