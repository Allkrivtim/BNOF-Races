# 05. `TickService` — единый scheduler "раз в секунду"

**Путь:** `src/main/java/dev/oneframe/races/tick/TickService.java`, `TickTask.java`, `TickTaskHandle.java`
**Зачем нужен:** это техническая реализация требования ТЗ "все периодические проверки — один фоновый scheduler раз в секунду". Вместо того чтобы каждая проверка (гипоксия, барьер, тик способностей, forced-имена) заводила свой собственный `Bukkit.getScheduler().runTaskTimer`, все они регистрируют себя здесь, а `TickService` крутит **один** таймер и раздаёт "такты" зарегистрированным задачам.

## `TickTask.java`

```java
package dev.oneframe.races.tick;

import java.util.function.Consumer;

/** intervalPasses = 1 means "every pass" (every 1s / 20 ticks); N means every Nth pass. */
public record TickTask(int intervalPasses, Consumer<Long> action) {
}
```

- `record` с двумя полями: `intervalPasses` — через сколько "проходов" хартбита выполнять эту задачу (`1` — на каждом, `5` — на каждом пятом и т.д.), и `action` — сама логика, `Consumer<Long>` (функция, принимающая `Long` — номер текущего прохода — и ничего не возвращающая).
- Это чисто структура данных, никакой логики внутри — вся логика планирования в `TickService`.

## `TickTaskHandle.java`

```java
package dev.oneframe.races.tick;

public record TickTaskHandle(Runnable unregister) {
}
```

- Ещё один крошечный `record` — "квитанция" о регистрации задачи, оборачивающая `Runnable`, вызов которого отменяет регистрацию.
- **Важный нюанс:** в текущем коде плагина ни один вызывающий код фактически **не сохраняет** и не использует эту квитанцию (`OneFrameRacesPlugin#registerTickTasks` просто игнорирует возвращаемое значение `tickService.register(...)`) — то есть возможность отмены отдельной задачи заложена в API, но пока не используется. Это нормально: задачи живут всё время работы плагина и снимаются все разом при `tickService.stop()`. Если в будущем понадобится "включать/выключать" отдельное глобальное правило на лету — эта квитанция уже готова для такого случая.

## `TickService.java`

```java
package dev.oneframe.races.tick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
```

- `BukkitTask` — объект-хендл, который Bukkit возвращает при регистрации задачи в scheduler'е; через него можно отменить конкретно эту задачу (`cancel()`).
- `CopyOnWriteArrayList` — специальная потокобезопасная реализация списка, оптимизированная под сценарий "читаем часто, пишем редко": при любой модификации (`add`/`remove`) она **копирует весь внутренний массив**, а чтение (перебор `for`) вообще не требует блокировок и работает со "снимком" массива на момент начала итерации. Это идеальный выбор здесь: задачи регистрируются один раз при старте плагина (`registerTickTasks`, редкая операция записи), а вызываются 20 раз в минуту (частое чтение/перебор).

```java
/**
 * The single shared 1-second heartbeat behind every periodic check in the plugin (hypoxia,
 * barrier zones, named-item cleanup, forbidden-enchant sweeps, ability ticks, name enforcement).
 * Callers register a {@link TickTask} instead of starting their own
 * {@code Bukkit.getScheduler().runTaskTimer}; everything shares this one heartbeat.
 */
public final class TickService {

    private final Plugin plugin;
    private final List<TickTask> tasks = new CopyOnWriteArrayList<>();
    private long passCounter = 0;
    private BukkitTask heartbeat;

    public TickService(Plugin plugin) {
        this.plugin = plugin;
    }
```

- `plugin` — ссылка на владельца, нужна для двух вещей: регистрации в `Bukkit.getScheduler()` (Bukkit требует знать, какому плагину принадлежит задача) и логирования ошибок через `plugin.getLogger()`.
- `tasks` — список всех зарегистрированных подзадач.
- `passCounter` — счётчик "проходов" хартбита, растущий на 1 каждую секунду. Начинается с `0`.
- `heartbeat` — хендл самого таймера Bukkit, нужен, чтобы его можно было остановить в `stop()`.

```java
    public void start() {
        heartbeat = Bukkit.getScheduler().runTaskTimer(plugin, this::runPass, 20L, 20L);
    }
```

- `Bukkit.getScheduler()` — глобальный планировщик задач сервера (один на весь сервер, общий для всех плагинов).
- `runTaskTimer(plugin, this::runPass, 20L, 20L)` — регистрирует **повторяющуюся** синхронную задачу:
  - `plugin` — владелец.
  - `this::runPass` — метод-ссылка (method reference) на приватный метод `runPass()` этого же класса — эквивалент `() -> this.runPass()`, но короче. Именно этот метод будет вызываться на каждом такте таймера.
  - Первый `20L` — задержка перед **первым** запуском, в тиках (20 тиков = 1 секунда — то есть первый прогон случится не мгновенно при `start()`, а через секунду).
  - Второй `20L` — период **между** повторными запусками, тоже в тиках. Отсюда и берётся "раз в секунду": 20 тиков — константа игрового цикла Minecraft (см. [00-concepts.md](00-concepts.md#тики-и-scheduler-почему-раз-в-секунду--20-тиков)).
- Это единственное место во всём плагине, где вызывается `runTaskTimer` — то самое "один scheduler" из требования ТЗ.

```java
    public void stop() {
        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }
    }
```

- `heartbeat.cancel()` — останавливает повторяющуюся задачу в Bukkit-scheduler'е. Проверка на `!= null` защищает от двойного вызова `stop()` (например, если `onDisable()` вызвался бы больше одного раза — маловероятно, но не бесплатно проверить) и от вызова `stop()` до `start()`.
- Обнуление `heartbeat = null` после отмены — чтобы повторный вызов `stop()` не пытался снова звать `cancel()` на уже отменённой задаче (хотя это и не вызвало бы ошибки — Bukkit допускает повторный `cancel()`, — обнуление просто более аккуратный стиль).

```java
    public TickTaskHandle register(int intervalPasses, Consumer<Long> action) {
        TickTask task = new TickTask(Math.max(1, intervalPasses), action);
        tasks.add(task);
        return new TickTaskHandle(() -> tasks.remove(task));
    }
```

- Публичный API для регистрации новой периодической задачи.
- `Math.max(1, intervalPasses)` — защита от некорректного значения `0` или отрицательного (см. пример в [`OneFrameRacesPlugin`](02-main-plugin.md), где интервал вычисляется делением конфигурационного значения на 20 и мог бы получиться `0` при неверной настройке админом).
- `tasks.add(task)` — добавляем в список (при `CopyOnWriteArrayList` это создаёт новый внутренний массив-копию — операция дороже обычного `ArrayList.add`, но происходит только на старте плагина, не на каждом тике).
- `return new TickTaskHandle(() -> tasks.remove(task))` — возвращаем "квитанцию" с лямбдой, которая при вызове удалит именно эту задачу из списка. Замыкание (`closure`) здесь захватывает переменную `task` — конкретный объект `TickTask`, который был только что создан, — так что `unregister()` этой квитанции гарантированно уберёт именно ту задачу, для которой она была выдана, а не какую-то другую с тем же интервалом.

```java
    private void runPass() {
        passCounter++;
        for (TickTask task : tasks) {
            if (passCounter % task.intervalPasses() == 0) {
                try {
                    task.action().accept(passCounter);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "TickService task failed", ex);
                }
            }
        }
    }
}
```

Это сердце класса — вызывается ровно раз в секунду самим Bukkit-scheduler'ом:

- `passCounter++` — увеличиваем счётчик на 1 **до** проверки интервалов, то есть уже на первом вызове `passCounter` станет `1` (не `0`).
- `for (TickTask task : tasks)` — перебираем **все** зарегистрированные задачи (и "каждый проход", и "каждый N-й проход" — они все в одном списке).
- `passCounter % task.intervalPasses() == 0` — арифметика остатка от деления: если задача зарегистрирована с интервалом `1`, `passCounter % 1` всегда `0` (любое число делится на 1 без остатка) — то есть выполняется каждый раз. Если интервал `5` — выполняется на проходах `5, 10, 15, ...` (когда `passCounter` кратен 5). Это и есть механизм "разных интервалов на одном хартбите", о котором говорилось в [00-concepts.md](00-concepts.md) — единственный физический таймер тикает каждую секунду, а какие задачи реально выполнятся в конкретную секунду, решает эта проверка остатка.
- `task.action().accept(passCounter)` — вызываем саму лямбду задачи, передавая ей текущий номер прохода.
- `try { ... } catch (Exception ex) { plugin.getLogger().log(...) }` — **критически важная защита**: если одна конкретная задача (скажем, проверка гипоксии) бросит исключение (например, из-за неожиданного `null` где-то в игровом API), это исключение **не должно** остановить весь `runPass()` и уж тем более не должно "убить" сам таймер Bukkit-scheduler'а (что как раз случилось бы, если бы исключение вылетело из метода, переданного в `runTaskTimer`, без перехвата — Bukkit в таком случае обычно просто логирует стектрейс и **отменяет** повторяющуюся задачу целиком). Обёртка `try/catch` вокруг **каждой отдельной** задачи (а не вокруг всего цикла) гарантирует, что падение одной проверки не помешает выполниться остальным в этом же проходе и не остановит весь хартбит навсегда.

---

**Как этот файл связан с уже разобранным:** [`OneFrameRacesPlugin#registerTickTasks`](02-main-plugin.md) — единственное место, где вызывается `register(...)`; там же создаётся сам `TickService` и вызывается `start()`/`stop()`. Задачи, которые здесь регистрируются, реализованы в [`RaceManager#tickAbilities`](04-registry-manager.md) (тик способностей рас) и в классах глобальных правил из [12-global-rules.md](12-global-rules.md).

**Дальше:** [06-storage-config.md](06-storage-config.md) — как назначения игрок→раса сохраняются на диск, и как читается `config.yml`.
