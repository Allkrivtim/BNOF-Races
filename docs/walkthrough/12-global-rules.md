# 12. Глобальные правила

- Высотная гипоксия реализована `BreathingService`: выше config-Y все неосвобождённые расы расходуют настоящий ванильный `remainingAir` от максимума до drowning-порога, видят шкалу пузырьков и затем получают обычный урон. На этой высоте сервис имеет приоритет над расовым дыханием Merman; `ALTITUDE_HYPOXIA` освобождает расу от правила — его несут Archangel и Seraphim (`AngelShared.EXEMPTIONS`).
- `BarrierZoneDeathRule`: считает только survival/adventure; creative/spectator сразу очищают счётчик.
- `DeepslateNoDropRule`: управляет drops deepslate-блоков; `LOW_Y_ORE_RULE` освобождает расу от правила — его несут Marinian и Fugu (`MermanShared.EXEMPTIONS`), Echo и Morkvald (`MonsterShared.EXEMPTIONS`).
- `ForbiddenEnchantRule`: фильтрует offers/loot/pickup и каждую секунду удаляет запрещённые stored enchants либо снимает обычные enchants. Валидные расовые предметы исключены.
- `PortalLockdownRule`: запрещает создание новых порталов, End platform, Ender Eye и любой teleport с destination в THE_END. Обычное использование огнива по obsidian не отменяется заранее.
- `TradeLockdownRule`: блокирует только Villager/WanderingTrader, не произвольные custom Merchant GUI.
- `NameEnforcementRule`: пишет display/list name только при фактическом отличии.

Правила, которые работают в heartbeat, изолированы друг от друга. Concurrent-коллекции не используются там, где доступ строго однопоточный.
