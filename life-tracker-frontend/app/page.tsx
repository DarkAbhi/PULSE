import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth/session";
import { SignInScreen } from "@/features/auth/screens/sign-in-screen";

export default async function LoginPage() {
  if (await getSession()) redirect("/dashboard");
  return <SignInScreen />;
}
