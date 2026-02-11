"use client";

import { useEffect, useRef } from "react";
import { useAuth } from "@/providers/auth-provider";
import { initPushNotifications } from "@/lib/push";

export function PushInitializer() {
  const { authenticated, token } = useAuth();
  const initialized = useRef(false);

  useEffect(() => {
    if (!authenticated || !token || initialized.current) return;

    initialized.current = true;
    initPushNotifications(token).catch(console.error);
  }, [authenticated, token]);

  return null;
}
