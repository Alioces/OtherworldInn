package com.otherworldinn.util.service;

import java.util.List;
import net.minecraft.util.RandomSource;

public final class SponsorNamePool {
    public static final List<String> NAMES =
            List.of(
                    "MOQing",
                    "YeLuoYueShang",
                    "Ailisi415",
                    "PCLuige",
                    "SPDish",
                    "KnightKite",
                    "YOYIMIYASAMA",
                    "Yiyi",
                    "Mangomineralspr",
                    "AngryB",
                    "Xiwanzi",
                    "Hkszk",
                    "QvQi",
                    "GimmickyHare220",
                    "YoungAubthur",
                    "Kevinoer",
                    "AoRan",
                    "Lazypeople",
                    "RIHE122",
                    "NekotuanQAQ",
                    "HuLi_Tn",
                    "LinLei_Baruch",
                    "DanxiaoHanser",
                    "TheFlareStar",
                    "Biantwin",
                    "Striveturtle",
                    "XHeYa_3u3",
                    "Wuyu_OWO",
                    "chihuo_QWQ",
                    "B_eibao",
                    "Mr_eyes_5",
                    "JustKayina",
                    "IsoiaYUME",
                    "Cadivy",
                    "Yumicuibb",
                    "KeyxelDesu",
                    "Caoning",
                    "NomeSun");

    private SponsorNamePool() {}

    public static String getRandomName(RandomSource random) {
        return NAMES.get(random.nextInt(NAMES.size()));
    }
}
