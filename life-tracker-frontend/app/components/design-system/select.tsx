import type { ComponentPropsWithRef } from "react";
import styles from "./select.module.css";

export default function Select({ className = "", ...props }: ComponentPropsWithRef<"select">) {
  return <select {...props} className={`${styles.select} ${className}`} />;
}
