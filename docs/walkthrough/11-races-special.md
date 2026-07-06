# 11. Раса SPECIAL: Skyborn и Underground

**Путь:** `src/main/java/dev/oneframe/races/races/special/SkybornProvider.java`, `UndergroundProvider.java`
**Зачем нужен:** это самые простые расы в проекте — ни у одной нет ни одной способности в списке `abilities()`. Они хорошо иллюстрируют "нижнюю границу" контракта `RaceProvider`: минимально возможная валидная реализация.

## `SkybornProvider.java`

```java
package dev.oneframe.races.races.special;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;

import java.util.List;
import java.util.Set;

public final class SkybornProvider implements RaceProvider {

    @Override
    public String id() {
        return "skyborn";
    }

    @Override
    public String displayName() {
        return "Skyborn";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.SPECIAL;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 20;
    }

    @Override
    public double sp() {
        return 0;
    }
```

- Стандартные `hp()`/`sp()` — `20`/`0` (10 сердечек, без брони) — согласно ТЗ.

```java
    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return Set.of(ExemptionFlag.ALTITUDE_HYPOXIA);
    }

    @Override
    public List<Ability> abilities() {
        return List.of();
    }
}
```

- **Это единственная строчка, ради которой существует вся раса Skyborn:** `Set.of(ExemptionFlag.ALTITUDE_HYPOXIA)` — набор из одного флага, дающий иммунитет к высотной гипоксии (глобальное правило 1, см. [12-global-rules.md](12-global-rules.md)). Согласно ТЗ, у Skyborn "только иммунитет к высотной гипоксии" — то есть буквально никакой другой механики нет.
- `abilities()` возвращает `List.of()` — **пустой неизменяемый список**. Это валидная реализация контракта: [`RaceManager#applyRace`](04-registry-manager.md) и любой listener, перебирающий `race.abilities()`, просто не найдёт в этом пустом списке ни одного `PassiveEffectAbility`/`TickAbility`/специфичной способности — цикл `for (Ability ability : race.abilities())` просто не выполнит тело ни разу. Никакого специального "если список пуст" кода в остальной части плагина не требуется — пустая коллекция обрабатывается сама собой благодаря тому, что весь остальной код полагается на итерацию, а не на прямой доступ по индексу.
- **Как именно работает иммунитет:** сама раса **не содержит** никакой логики, реализующей иммунитет — она просто **декларирует флаг**. Реальная проверка происходит в другом файле — [`AltitudeHypoxiaRule#tick`](12-global-rules.md), которая при каждом проходе спрашивает `race.exemptionFlags().contains(ExemptionFlag.ALTITUDE_HYPOXIA)` у активной расы игрока и, если это истина, просто не применяет никакого урона/траты кислорода. Это иллюстрирует общий архитектурный принцип плагина: **раса объявляет "что она есть" (флаги, список способностей), а не "как реализовано" правило** — саму механику правила реализует единственный класс правила, общий для всех рас.

## `UndergroundProvider.java`

```java
package dev.oneframe.races.races.special;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;

import java.util.List;
import java.util.Set;

/** Stub race: no unique mechanic yet. Future abilities are additions to {@link #abilities()}. */
public final class UndergroundProvider implements RaceProvider {

    @Override
    public String id() {
        return "underground";
    }

    @Override
    public String displayName() {
        return "Underground";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.SPECIAL;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 20;
    }

    @Override
    public double sp() {
        return 0;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return Set.of();
    }

    @Override
    public List<Ability> abilities() {
        return List.of();
    }
}
```

- Это **буквально заготовка** (stub) — javadoc над классом прямо это признаёт: "Stub race: no unique mechanic yet." ТЗ описывает Underground как "заготовку" ("пока без уникальной механики").
- В отличие от Skyborn (у которого пуст только `abilities()`, но есть содержательный `exemptionFlags()`), у Underground **оба** метода возвращают пустые/нулевые значения: `exemptionFlags() = Set.of()` (ни от чего не освобождён) и `abilities() = List.of()` (никаких способностей).
- **Зачем вообще нужна раса, которая ничего не делает?** Чтобы её можно было **назначать** игрокам уже сейчас (через `/race set <игрок> underground`), она полноценно участвует во всей общей инфраструктуре (лимит `maxPlayers`, сохранение в `races.yml`, отображение в `/race list`/`/race info`, применение стандартных HP/брони) — просто без специфичной механики. Когда для неё придумают уникальную способность, потребуется **только** добавить новые классы способностей в пакет `special` и дописать их в список внутри `abilities()` — вся остальная инфраструктура (регистрация, применение, снятие при смене расы) уже готова и не требует изменений. Это прямая демонстрация того, зачем в архитектуре разделены "контракт расы" и "реализация конкретных правил/эффектов" — расширение одной расы новой способностью никогда не требует правки других файлов, кроме, возможно, регистрации нового listener'а, если способность реагирует на новый тип события (см. чек-лист "как добавить способность существующей расе" в `CLAUDE.md`).

---

**Как этот файл связан с уже разобранным:** обе расы реализуют минимальный [`RaceProvider`](03-core-interfaces.md); `SkybornProvider.exemptionFlags()` напрямую влияет на поведение [`AltitudeHypoxiaRule`](12-global-rules.md), разбираемого в следующем разделе.

**Дальше:** [12-global-rules.md](12-global-rules.md) — семь глобальных правил, применяемых ко всем игрокам (кроме тех, кто явно от них освобождён).
