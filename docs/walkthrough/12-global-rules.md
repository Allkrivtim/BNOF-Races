# 12. Глобальные правила

- `AltitudeHypoxiaRule`: выше config-Y тратит внутренний воздух и наносит обычный урон.
- `BarrierZoneDeathRule`: считает только survival/adventure; creative/spectator сразу очищают счётчик.
- `DeepslateNoDropRule`: управляет drops deepslate-блоков.
- `ForbiddenEnchantRule`: фильтрует offers/loot/pickup и каждую секунду удаляет запрещённые stored enchants либо снимает обычные enchants. Валидные расовые предметы исключены.
- `PortalLockdownRule`: запрещает создание новых порталов, End platform, Ender Eye и любой teleport с destination в THE_END. Обычное использование огнива по obsidian не отменяется заранее.
- `TradeLockdownRule`: блокирует только Villager/WanderingTrader, не произвольные custom Merchant GUI.
- `NameEnforcementRule`: пишет display/list name только при фактическом отличии.

Правила, которые работают в heartbeat, изолированы друг от друга. Concurrent-коллекции не используются там, где доступ строго однопоточный.
