# 11. Категории и состав рас

В версии 2.0 зарегистрированы 10 рас:

- HUMAN: Forester, Blacksmith;
- MERMAN: Marinian, Fugu;
- DEMON: Blazeborn, Warlock;
- ANGEL: Archangel, Seraphim;
- MONSTER: Echo, Morkvald.

`RaceCategory.SPECIAL` и некоторые `ExemptionFlag` оставлены как совместимые точки расширения, но built-in SPECIAL-провайдеров сейчас нет. Старые walkthrough-упоминания Skyborn/Underground не относятся к текущему service-файлу и удалены.

Фактический перечень built-in рас всегда задаётся `src/main/resources/META-INF/services/dev.oneframe.races.core.RaceProvider`.
