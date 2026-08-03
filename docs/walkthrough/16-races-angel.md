# 16. Angel и Monster

Archangel и Seraphim получают собственные Elytra и Riptide-трезубец. На суше interaction откладывается на tick после отказа vanilla-проверки, затем `startUsingItem` запускает настоящее удержание. Отпускание раньше 10 тиков ничего не делает; после порога вычисляется vanilla Riptide III impulse 3.0, вызывается cancellable `PlayerRiptideEvent`, добавляется velocity и запускается `startRiptideAttack(20, 8, item)`. Это даёт настоящую раскрутку, collision attack и звук Riptide III. После успешного сухого рывка `setCooldown(item, 70)` включает видимый кулдаун 3,5 секунды; ванильное мокрое использование само его не создаёт.

Archangel имеет 0 SP и ниже `settings.archangel-fatigue-below-y` получает бесконечный Mining Fatigue II; после подъёма эффект остаётся до внешнего очищения. Он не получает fall/kinetic damage и не начинает/продолжает glide во время горения. Seraphim не носит броню: исключение сделано только для собственных крыльев с валидными owner/race/item тегами; уже надетая броня снимается при применении расы и в heartbeat.

Благословение Серафима раз в секунду очищает у других игроков в радиусе 5 блоков только Slowness, Mining Fatigue, Nausea, Blindness, Hunger, Weakness, Poison, Wither, Unluck, Bad Omen, Darkness и Raid Omen. Положительные и остальные эффекты не затрагиваются; отдельного клиентского potion-effect «Благословение» нет.

Echo поддерживает тишину и отменяет свои `GenericGameEvent`, получает подземные/ночные бонусы. Маркер владения silent-состоянием позволяет при смене расы снять только тишину BNOF-Races. Morkvald имеет подземные/ночные бонусы и projectile immunity.

Все длительные расовые пассивы используют `PotionEffect.INFINITE_DURATION`. Условные эффекты добавляются только при входе в условие и не снимаются при выходе; после молока/очищения они не возвращаются на каждом heartbeat. Instantaneous Saturation и активные временные эффекты атак/предметов остаются конечными по своей природе.
