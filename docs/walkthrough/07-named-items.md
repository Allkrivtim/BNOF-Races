# 07. Расовые предметы

`NamedItemDefinition` хранит item key, race id и фабрику шаблона. `NamedItemService#createTagged` добавляет PDC:

- `bnof-races:named_owner`;
- `bnof-races:named_race`;
- `bnof-races:named_item_key`;
- `bnof-races:named_schema = 2`.

Предмет считается расовым только при валидных всех трёх полях идентичности. Legacy-ключи `oneframe:*` читаются, но секундная reconciliation пересоздаёт предмет по актуальному шаблону/schema.

Reconciliation удаляет предметы чужого владельца/расы, неизвестные ключи, повреждённые теги и дубликаты, затем выдаёт недостающее. Если слот экипировки занят, предмет идёт в инвентарь; overflow выбрасывается у игрока.

Transfer guard блокирует контейнеры, drop, hopper и чужой pickup. На смерти предметы удаляются из drops/inventory и выдаются заново после respawn. Активация проверяет owner, race и item key, а не Material/display name.
