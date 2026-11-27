import { redirect } from "next/navigation";

/** The home page is the dashboard. */
export default function Home() {
  redirect("/dashboard");
}
