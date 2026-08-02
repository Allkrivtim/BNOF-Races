# 14. Команды

`RaceCommand` реализует list, info, get, set, clear и reload. Admin-проверка принимает `bnof.race.admin` и legacy `oneframe.race.admin`.

`set` проверяет provider и occupancy. `set`/`clear` сохраняют snapshot вне main thread и выводят успех только после durable save; `SAVE_FAILED` оставляет прежнее назначение, `BUSY` защищает порядок мутаций.

`reload`:

1. сохраняет старые providers онлайн-игроков;
2. перечитывает `config.yml`;
3. пересобирает registry/addon classloaders;
4. перечитывает `races.yml`;
5. очищает старое и применяет новое состояние онлайн-игроков.

`RaceTabCompleter` берёт ids из актуального registry.
