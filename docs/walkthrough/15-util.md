# 15. Утилиты

`AttributeUtil` не меняет base values. Он ставит именованные transient `ADD_NUMBER` modifiers так, чтобы итог соответствовал HP/armor/toughness расы, а при очистке удаляет только ключи `bnof-races:*`.

Так же реализовано отсутствие подводного штрафа: race modifier удаляется при смене расы, не затирая значения другого плагина.

`InventoryUtil.giveOrDrop` возвращает остаток `Inventory#addItem` в мир рядом с игроком. `EnchantPools` централизует список запрещённых чар, `WorldTimeUtil` — проверку ночи, `Msg` — формат сообщений.
