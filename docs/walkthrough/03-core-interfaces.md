# 03. Контракты core

- `RaceProvider` — паспорт расы.
- `Ability` — описание и объявление принадлежащих способности potion-effect типов.
- `PassiveEffectAbility` — безусловные бесконечные эффекты.
- `ConditionalPassiveEffectAbility` — бесконечные эффекты, применяемые на false→true переходе условия и остающиеся после выхода из него.
- `TickAbility` — `onApply` и секундный `tick`.
- `EventAbilities` — небольшие интерфейсы для damage, attack, consume, armor, glide, interact и других событий.
- `AbilityContext` — номер прохода, config и `RaceManager`.
- `ExemptionFlag` — явные исключения из глобальных правил.

Сторонняя событийная способность должна реализовать соответствующий `EventAbilities.*`. Центральный listener увидит её без импорта конкретного класса. Для нового типа события сначала добавляется capability-интерфейс и один центральный listener.

`ownedPotionEffects()` используется для очистки состояния при смене расы. Молоко, `/effect clear` и Blessing работают: эффект не восстанавливается каждую секунду, а ждёт assignment/join/respawn/world-change/wake-up, пятиминутного refresh либо нового входа в собственное условие.
