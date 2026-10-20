"use client";

import { useEffect, useState } from "react";

interface LocalDateProps {
  dateString: string;
  options?: Intl.DateTimeFormatOptions;
}

export default function LocalDate({ dateString, options }: LocalDateProps) {
  const [formatted, setFormatted] = useState("");

  useEffect(() => {
    const formatter = new Intl.DateTimeFormat("en-IN", options ?? {
      dateStyle: "medium",
      timeStyle: "short",
    });
    setFormatted(formatter.format(new Date(dateString)));
  }, [dateString, options]);

  return <span suppressHydrationWarning>{formatted}</span>;
}
