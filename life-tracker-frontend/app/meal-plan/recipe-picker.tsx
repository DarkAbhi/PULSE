"use client";

import { useEffect, useId, useState } from "react";
import { ChevronLeft, ChevronRight, Clock, Search } from "lucide-react";
import Button from "@/app/components/design-system/button";
import { searchRecipesAction, type RecipeSummary } from "@/app/meal-plan/actions";
import { mealPlanStyles as styles } from "@/app/meal-plan/meal-plan-classes";

function RecipeMetadata({ recipe }: { recipe: RecipeSummary }) {
  return (
    <span className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
      {recipe.dish_type && <span>{recipe.dish_type}</span>}
      {recipe.is_ready_to_eat ? <span>Ready to eat</span> : !!recipe.ready_in_minutes && (
        <span className="inline-flex items-center gap-1"><Clock className="size-3.5" />{recipe.ready_in_minutes} min</span>
      )}
    </span>
  );
}

export function RecipePicker({ onSelect }: { onSelect: (recipe: RecipeSummary) => void }) {
  const field = useId();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState<{
    search: string;
    page: number;
    retry: number;
    value: Awaited<ReturnType<typeof searchRecipesAction>>;
  }>();

  useEffect(() => {
    let current = true;
    const timer = window.setTimeout(async () => {
      const value = await searchRecipesAction(search.trim(), page);
      if (current) setResult({ search, page, retry, value });
    }, search.trim() ? 250 : 0);
    return () => {
      current = false;
      window.clearTimeout(timer);
    };
  }, [search, page, retry]);

  const response = result?.search === search && result.page === page && result.retry === retry ? result.value : undefined;

  return (
    <div className="space-y-3">
      <label htmlFor={field} className="block text-sm font-semibold">Search recipes</label>
      <div className="relative">
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
        <input
          id={field}
          type="search"
          value={search}
          autoFocus
          maxLength={100}
          onChange={(event) => { setSearch(event.target.value); setPage(1); }}
          placeholder="Search by dish, cuisine or meal time"
          className="min-h-11 w-full rounded-lg bg-secondary py-2 pr-3 pl-9 text-sm text-foreground placeholder:text-muted-foreground"
        />
      </div>
      {response === undefined ? <p role="status" className="py-2 text-sm text-muted-foreground">Loading recipes…</p>
        : response === null ? (
          <div role="alert" className="flex items-center justify-between gap-2">
            <p className="text-sm text-destructive">Could not load recipes.</p>
            <Button type="button" variant="secondary" onClick={() => setRetry(retry + 1)}>Retry</Button>
          </div>
        ) : response.recipes.length === 0 ? (
          <p role="status" className="py-2 text-sm text-muted-foreground">No recipes found.</p>
        ) : (
          <>
            <ul className={styles.recipeResults} aria-label="Recipes">
              {response.recipes.map((recipe) => (
                <li key={recipe.id}>
                  <button className={styles.recipeResult} type="button" onClick={() => onSelect(recipe)}>
                    <span className="min-w-0 space-y-1">
                      <span className="block text-sm font-bold wrap-anywhere">
                        {recipe.name}
                      </span>
                      <RecipeMetadata recipe={recipe} />
                    </span>
                    <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>
            {(page > 1 || response.has_more) && (
              <nav className="flex items-center justify-between gap-2" aria-label="Recipe results pages">
                <Button type="button" variant="secondary" disabled={page === 1} onClick={() => setPage(page - 1)} aria-label="Previous recipe page">
                  <ChevronLeft className="size-4" aria-hidden="true" />
                </Button>
                <span className="text-xs text-muted-foreground">Page {page}</span>
                <Button type="button" variant="secondary" disabled={!response.has_more} onClick={() => setPage(page + 1)} aria-label="Next recipe page">
                  <ChevronRight className="size-4" aria-hidden="true" />
                </Button>
              </nav>
            )}
          </>
        )}
    </div>
  );
}
