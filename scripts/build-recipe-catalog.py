#!/usr/bin/env python3
"""Build migration 00045 from MongoDB JSON-array exports named by collection (no live DB needed)."""

import argparse
from collections import Counter, defaultdict
from html import unescape
import json
from pathlib import Path
import re

LOOKUPS = {
    "recipe_category": "category",
    "tags": "tag",
    "preferred_for": "meal_time",
    "appliance": "equipment",
    "recipe_collections": "collection",
    "ingredient_category": "ingredient_category",
    "ingredient_found_usually": "ingredient_storage",
    "expert_plan_category": "plan_category",
}
# Published, single-ingredient foods with no preparation steps in the source catalog.
READY_TO_EAT = {
    "Kiwi", "Papaya", "Watermelon", "Roasted Makhana", "Orange", "Pomegranate",
    "Pear", "Roasted Chana", "Sprouted Green Moong", "Khakhra", "Murmura Bhel",
    "Boiled Chana", "Mango",
}
PLACEHOLDER = re.compile(r"\b(test(?:ing)?|dummy|lorem ipsum|asdf|demo)\b", re.I)
OBJECT_ID = re.compile(r"[0-9a-f]{24}")


def oid(value):
    if isinstance(value, dict):
        value = value.get("$oid")
    if isinstance(value, str) and OBJECT_ID.fullmatch(value):
        return value
    return None


def text(value):
    if not isinstance(value, str):
        return ""
    value = unescape(value)
    value = re.sub(r"<br\s*/?>|</(?:p|div|li)>", "\n", value, flags=re.I)
    value = re.sub(r"<[/!]?[a-z][^>]*>", "", value, flags=re.I)
    value = re.sub(r"https?://\S+", "", value)
    return value.replace("\x00", "").strip()


def fields(record, names):
    return {key: record[key] for key in names.split() if record.get(key) is not None}


def refs(record, field):
    values = record.get(field) or []
    return [oid(value) for value in (values if isinstance(values, list) else [values])]


def quantities(owner, entries, ingredients):
    for position, amount in enumerate(entries, 1):
        ingredient_id = oid(amount.get("id"))
        definition = ingredients.get(ingredient_id)
        name = text(amount.get("name")) or (text(definition["name"]) if definition else "")
        if not name or PLACEHOLDER.search(name):
            raise ValueError("ingredient placeholder or missing name")
        if not isinstance(amount.get("qty"), (int, float)) or not text(amount.get("unit")):
            raise ValueError("ingredient quantity or unit missing")
        yield (owner, position, ingredient_id if definition else None, name,
               amount["qty"], text(amount["unit"]), bool(amount.get("important")))


def order_steps(steps):
    by_id = {oid(step["_id"]): step for step in steps}
    dependencies = {
        step_id: {target for target in refs(step, "preceding_steps") if target in by_id and target != step_id}
        for step_id, step in by_id.items()
    }
    for step_id, step in by_id.items():
        for target in refs(step, "consecutive_steps"):
            if target in by_id and target != step_id:
                dependencies[target].add(step_id)

    def sort_key(step_id):
        name = by_id[step_id]["name"]
        prefix = re.match(r"^\s*([pm])\s*(\d+)", name, re.I)
        number = int(prefix[2]) if prefix else 0
        return (by_id[step_id]["type"] != "prep_method", number, step_id)

    ordered = []
    while dependencies:
        ready = [step_id for step_id, previous in dependencies.items() if not previous]
        if not ready:
            raise ValueError("cyclic step dependencies")
        step_id = min(ready, key=sort_key)
        ordered.append(by_id[step_id])
        del dependencies[step_id]
        for previous in dependencies.values():
            previous.discard(step_id)
    return ordered


def build(data):
    rows = defaultdict(list)
    rejected = Counter()
    labels = {
        oid(record["_id"]): (kind, record, text(record.get("title") or record.get("name")))
        for collection, kind in LOOKUPS.items()
        for record in data[collection]
    }
    ingredients = {
        oid(record["_id"]): record for record in data["ingredient"]
        if text(record["name"]) and not PLACEHOLDER.search(record["name"])
    }
    source_steps = defaultdict(list)
    for step in data["recipe_steps"]:
        if step["active"]:
            source_steps[oid(step["recipe"])].append(step)

    for recipe in data["recipe"]:
        recipe_id = oid(recipe["_id"])
        name = text(recipe["name"]).rstrip("*").strip()
        description = text(recipe["description"])
        if not recipe["active"]:
            rejected["inactive"] += 1
            continue
        if not name or not description or PLACEHOLDER.search(name):
            rejected["placeholder or empty recipe"] += 1
            continue
        steps = source_steps[recipe_id]
        try:
            if any(not text(step["name"]) or len(text(step["description"])) < 12
                   or PLACEHOLDER.fullmatch(text(step["description"])) for step in steps):
                raise ValueError("missing step instructions")
            steps = order_steps(steps)
            amounts = list(quantities(recipe_id, recipe.get("ingredients") or [], ingredients))
            step_amounts = [
                amount
                for step in steps
                for amount in quantities(oid(step["_id"]), step.get("ingredients") or [], ingredients)
            ]
            # Some source records keep quantities only on their cooking steps.
            if not amounts and not step_amounts:
                raise ValueError("missing ingredients")
            if not steps and (name not in READY_TO_EAT or len(amounts) != 1):
                raise ValueError("missing steps")
        except ValueError as err:
            rejected[str(err)] += 1
            continue

        aliases = list(dict.fromkeys(clean for alias in recipe.get("alternate_name") or []
                                    if (clean := text(alias))))
        details = fields(recipe, "active_time total_prep_time difficulty_level nutrients ratings")
        rows["catalog_recipes"].append((recipe_id, name, description, text(recipe["type"]),
                                        not steps, aliases, recipe.get("default_recipe"),
                                        recipe.get("ready_in"), details))
        rows["catalog_recipe_ingredient_amounts"].extend(amounts)
        rows["catalog_step_ingredient_amounts"].extend(step_amounts)
        step_ids = {oid(step["_id"]) for step in steps}
        for position, step in enumerate(steps, 1):
            step_id = oid(step["_id"])
            details = fields(step, "active_time semi_active_time idle_time advanced_prep_time timer flame")
            rows["catalog_recipe_steps"].append((step_id, recipe_id, position, text(step["name"]),
                                                 text(step["description"]), step["type"], details))
            for field, kind in (("preceding_steps", "preceding"), ("consecutive_steps", "consecutive")):
                for index, target in enumerate(dict.fromkeys(refs(step, field)), 1):
                    if target in step_ids and target != step_id:
                        rows["catalog_recipe_step_links"].append((recipe_id, step_id, target, kind, index))
                    else:
                        rejected["unavailable step links"] += 1
            for index, target in enumerate(dict.fromkeys(refs(step, "equipments")), 1):
                if target in labels:
                    rows["catalog_recipe_step_equipment"].append((step_id, target, index))

    recipe_ids = {row[0] for row in rows["catalog_recipes"]}
    used_ingredients = {row[2] for table in ("catalog_recipe_ingredient_amounts", "catalog_step_ingredient_amounts")
                        for row in rows[table] if row[2]}
    ingredient_label_ids = set()
    for ingredient_id in sorted(used_ingredients):
        source = ingredients[ingredient_id]
        category, storage = oid(source.get("category")), oid(source.get("found_usually"))
        category = category if category in labels else None
        storage = storage if storage in labels else None
        ingredient_label_ids.update((category, storage))
        details = fields(source, "sku min_order_qty min_order_unit main_unit weight_per_unit cooking_units")
        details.update(description=text(source["description"]),
                       other_name=[text(alias) for alias in source.get("other_name") or []])
        rows["catalog_ingredients"].append((ingredient_id, text(source["name"]), category, storage, details))

    for recipe in data["recipe"]:
        recipe_id = oid(recipe["_id"])
        if recipe_id not in recipe_ids:
            continue
        for field in ("categories", "tags", "preferred_for", "equipments"):
            for index, target in enumerate(dict.fromkeys(refs(recipe, field)), 1):
                if target in labels:
                    rows["catalog_recipe_label_links"].append((recipe_id, target, index))
                else:
                    rejected["unavailable recipe labels"] += 1
        for field, kind in (("combine_with", "pairing"), ("similar_recipe", "similar")):
            for index, target in enumerate(dict.fromkeys(refs(recipe, field)), 1):
                if target in recipe_ids and target != recipe_id:
                    rows["catalog_recipe_links"].append((recipe_id, target, kind, index))
                else:
                    rejected["unavailable or self recipe links"] += 1

    for collection in data["recipe_collections"]:
        if collection["active"]:
            for index, target in enumerate(dict.fromkeys(refs(collection, "similar_recipe")), 1):
                if target in recipe_ids:
                    rows["catalog_recipe_label_links"].append((target, oid(collection["_id"]), index))

    for template in data["expert_plan"]:
        # Personal plans also exist without for_users. Only reviewed generic plans qualify.
        name = text(template["plan_name"])
        if not template["active"] or template.get("for_users") or name not in {
            "Mostly North Indian", "Mostly South Indian", "3 day meal plan", "Lean Machine Meal Plan",
        }:
            continue
        template_id = oid(template["_id"])
        days = [day for day in data["expert_plan_days"] if day["active"] and oid(day["expert"]) == template_id]
        meals = [(template_id, day["day"], oid(slot["preferred_for"]), position, oid(recipe))
                 for day in days for slot in day["plan"] for position, recipe in enumerate(slot["recipes"], 1)]
        # Keep only complete generic plans. Never swap a missing recipe or change a slot.
        if not meals or any(meal[2] not in labels or meal[4] not in recipe_ids for meal in meals):
            rejected["generic templates with unavailable recipes"] += 1
            continue
        category = oid(template.get("category"))
        rows["catalog_meal_templates"].append((template_id, name, text(template["description"]),
                                               "", category if category in labels else None))
        rows["catalog_meal_template_recipes"].extend(meals)

    used_labels = (ingredient_label_ids | {row[1] for row in rows["catalog_recipe_label_links"]}
                   | {row[1] for row in rows["catalog_recipe_step_equipment"]}
                   | {row[4] for row in rows["catalog_meal_templates"]}
                   | {row[2] for row in rows["catalog_meal_template_recipes"]})
    for label_id in sorted(used_labels - {None}):
        kind, record, name = labels[label_id]
        rows["catalog_recipe_labels"].append((label_id, kind, name, fields(record, "order home_page sequence")))
    return rows, rejected


# Column order matches migration 00044.
TABLES = (
    "catalog_recipes", "catalog_recipe_labels", "catalog_recipe_label_links", "catalog_ingredients",
    "catalog_recipe_steps", "catalog_recipe_step_links", "catalog_recipe_step_equipment",
    "catalog_recipe_ingredient_amounts", "catalog_step_ingredient_amounts", "catalog_recipe_links",
    "catalog_meal_templates", "catalog_meal_template_recipes",
)


def literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return json.dumps(value, allow_nan=False)
    if isinstance(value, list):
        return "ARRAY[" + ",".join(map(literal, value)) + "]::text[]"
    if isinstance(value, dict):
        value = json.dumps(value, ensure_ascii=False, separators=(",", ":"), allow_nan=False)
    return "'" + value.replace("'", "''") + "'"


def write_migrations(target, rows):
    header = "-- Generated from the sanitized recipe catalog snapshot by scripts/build-recipe-catalog.py.\n"
    with (target / "00045_recipe_catalog_seed.up.sql").open("w", encoding="utf-8") as stream:
        stream.write(header)
        for table in TABLES:
            records = sorted(rows[table], key=lambda row: row[:1])
            for start in range(0, len(records), 500):
                batch = records[start:start + 500]
                values = [",".join("(" + ",".join(map(literal, row)) + ")" for row in batch[i:i + 5])
                          for i in range(0, len(batch), 5)]
                stream.write(f"INSERT INTO {table} VALUES\n" + ",\n".join(values) + ";\n\n")
    with (target / "00045_recipe_catalog_seed.down.sql").open("w", encoding="utf-8") as stream:
        stream.write(header)
        for table in ("catalog_meal_templates", "catalog_recipes", "catalog_ingredients", "catalog_recipe_labels"):
            ids = sorted(row[0] for row in rows[table])
            if ids:
                groups = [",".join(map(literal, ids[i:i + 10])) for i in range(0, len(ids), 10)]
                stream.write(f"DELETE FROM {table} WHERE id IN (\n" + ",\n".join(groups) + "\n);\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("exports", type=Path)
    parser.add_argument("migrations", type=Path)
    args = parser.parse_args()
    collections = set(LOOKUPS) | {"recipe", "recipe_steps", "ingredient", "expert_plan", "expert_plan_days"}
    data = {name: json.loads((args.exports / f"{name}.json").read_text(encoding="utf-8"))
            for name in collections}
    rows, skipped = build(data)
    if not rows["catalog_recipes"]:
        parser.error("the export produced no usable recipes")
    write_migrations(args.migrations, rows)
    print(json.dumps({"inserted": {table: len(rows[table]) for table in TABLES}, "skipped": skipped}, indent=2))


if __name__ == "__main__":
    main()
