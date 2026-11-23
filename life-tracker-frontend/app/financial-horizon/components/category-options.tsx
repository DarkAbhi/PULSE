import type { CategoryItem } from "@/app/financial-horizon/horizon-types";

export default function CategoryOptions({ categories }: { categories: CategoryItem[] }) {
  const children = new Map<number, CategoryItem[]>();
  for (const category of categories) {
    if (category.parent_id == null) continue;
    const siblings = children.get(category.parent_id) ?? [];
    siblings.push(category);
    children.set(category.parent_id, siblings);
  }

  return categories.filter(category => category.parent_id == null).map(parent => {
    const subcategories = children.get(parent.id);
    if (!subcategories?.length) {
      return <option key={parent.id} value={parent.id}>{parent.name}</option>;
    }
    return (
      <optgroup key={parent.id} label={parent.name}>
        <option value={parent.id}>{parent.name}</option>
        {subcategories.map(child => (
          <option key={child.id} value={child.id}>{parent.name} / {child.name}</option>
        ))}
      </optgroup>
    );
  });
}
