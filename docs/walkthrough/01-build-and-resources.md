# 01. Сборка и ресурсы

Gradle собирает Java 21-проект с `compileOnly` Paper API 1.21.11. Shadow не нужен: сторонних runtime-библиотек нет.

```bash
./gradlew clean build
```

Плагин называется `BNOF-Races-2.0.jar`; задача `resourcePack` параллельно собирает `BNOF-Races-ResourcePack.zip`. `plugin.yml` объявляет `BnofRacesPlugin`, команду `/race`, новые права `bnof.race.*` и legacy-права `oneframe.race.*`.

Ресурсы:

- `config.yml` — настройки правил и установщика датапака;
- `META-INF/services/...RaceProvider` — 10 built-in провайдеров;
- `datapack/bnof-races-height/` — единственный исходник высотного пакета.

Внешняя папка `resourcepack/BNOF-Races/` содержит объединённый клиентский пак: исходные server-icon/font пользователя плюс глобальную замену текстуры и переводов `minecraft:milk_bucket`. Она намеренно не упаковывается внутрь jar, потому что клиент получает ресурспак по серверному URL.

`processResources` подставляет Gradle-версию в `plugin.yml`. Ожидаемое содержимое jar проверяется командой `jar tf build/libs/BNOF-Races-2.0.jar`, а содержимое клиентского пакета — `unzip -l build/libs/BNOF-Races-ResourcePack.zip`.
