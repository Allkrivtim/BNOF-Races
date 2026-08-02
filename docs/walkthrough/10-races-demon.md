# 10. Demon

`Blazeborn` связан с огнём и Nether: огненные стрелы, поджигание цели, штраф воды, урон вне Nether и посмертный взрыв. Расовый periodic damage больше не сбрасывает `noDamageTicks`, поэтому не ломает vanilla/плагинные i-frames.

`Warlock` имеет вампирический удар, иммунитет к Wither и расовые незеритовые ботинки. Damage, potion, projectile и death логика подключается через `EventAbilities`.

Визуальный посмертный взрыв не разрушает блоки; урон наносится найденным живым сущностям через обычный Bukkit damage pipeline.
