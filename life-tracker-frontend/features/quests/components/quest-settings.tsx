"use client";

import { useState } from "react";
import { Settings2 } from "lucide-react";
import PulseButton from "@/components/ui/pulse-button";
import PulseIconButton from "@/components/ui/pulse-icon-button";
import QuestGoalsDialog, { type QuestGoalsSettings } from "@/features/quests/components/quest-goals-dialog";

export default function QuestSettingsButton(props: QuestGoalsSettings) {
  const [open, setOpen] = useState(false);
  return (
    <>
      {props.configured ? (
        <PulseIconButton type="button" variant="overlay" size="lg" aria-label="Edit quest goals"
          className="bg-(--quest-panel) text-(--quest-text) shadow-(--quest-shadow)" onClick={() => setOpen(true)}>
          <Settings2 className="size-5" aria-hidden="true" />
        </PulseIconButton>
      ) : (
        <PulseButton type="button" size="lg" onClick={() => setOpen(true)} icon={<Settings2 className="size-5" aria-hidden="true" />}>
          Set quest goals
        </PulseButton>
      )}
      {open && <QuestGoalsDialog {...props} close={() => setOpen(false)} />}
    </>
  );
}
