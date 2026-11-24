"use client";

import { useState } from "react";
import { Settings2 } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import QuestGoalsDialog, { type QuestGoalsSettings } from "@/app/quests/quest-goals-dialog";

export default function QuestSettingsButton(props: QuestGoalsSettings) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <PulseButton type="button" variant={props.configured ? "iconOverlay" : "primary"} size="lg"
        aria-label={props.configured ? "Edit quest goals" : undefined}
        className={props.configured ? "bg-(--quest-panel) text-(--quest-text) shadow-(--quest-shadow)" : undefined}
        onClick={() => setOpen(true)}
        icon={<Settings2 className="size-5" aria-hidden="true" />}>
        {!props.configured && "Set quest goals"}
      </PulseButton>
      {open && <QuestGoalsDialog {...props} close={() => setOpen(false)} />}
    </>
  );
}
