# 01. Сборка и ресурсы

Gradle собирает Java 21-проект с `compileOnly` Paper API 1.21.11. Shadow не нужен: сторонних runtime-библиотек нет.

```bash
./gradlew clean build
```

Архив называется `BNOF-Races-2.0.jar`. `plugin.yml` объявляет `BnofRacesPlugin`, команду `/race`, новые права `bnof.race.*` и legacy-права `oneframe.race.*`.

Ресурсы:

- `config.yml` — настройки правил и установщика датапака;
- `META-INF/services/...RaceProvider` — 10 built-in провайдеров;
- `datapack/bnof-races-height/` — единственный исходник высотного пакета.

`processResources` подставляет Gradle-версию в `plugin.yml`. Ожидаемое содержимое jar проверяется командой `jar tf build/libs/BNOF-Races-2.0.jar`.
