"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/providers/auth-provider";
import { LoadingScreen } from "@/components/layout/loading-screen";

export default function RootPage() {
  const { initialized, authenticated, hasRole } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!initialized || !authenticated) return;

    if (hasRole("prestador")) {
      router.replace("/prestador/dashboard");
    } else {
      router.replace("/cliente/home");
    }
  }, [initialized, authenticated, hasRole, router]);

  return <LoadingScreen />;
}
