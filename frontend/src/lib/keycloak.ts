import Keycloak from "keycloak-js";

let keycloakInstance: Keycloak | null = null;
let initPromise: Promise<boolean> | null = null;

export function getKeycloak(): Keycloak {
  if (typeof window === "undefined") {
    throw new Error("Keycloak can only be used in the browser");
  }

  if (!keycloakInstance) {
    keycloakInstance = new Keycloak({
      url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || "https://auth.iamgabrieldev.com.br",
      realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || "ajeitai",
      clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "ajeitai-frontend",
    });
  }

  return keycloakInstance;
}

/**
 * Initializes Keycloak only once, even across HMR and StrictMode re-renders.
 * Returns the same promise if init was already called.
 */
export function initKeycloak(kc: Keycloak): Promise<boolean> {
  if (!initPromise) {
    initPromise = kc.init({
      onLoad: "login-required",
      pkceMethod: "S256",
      checkLoginIframe: false,
    });
  }
  return initPromise;
}

export type UserRole = "cliente" | "prestador" | "admin";

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: UserRole[];
}

export function extractRoles(keycloak: Keycloak): UserRole[] {
  const realmAccess = keycloak.tokenParsed?.realm_access;
  if (!realmAccess?.roles) return [];

  const validRoles: UserRole[] = ["cliente", "prestador", "admin"];
  return realmAccess.roles.filter((r: string): r is UserRole =>
    validRoles.includes(r as UserRole)
  );
}

export function getUserProfile(keycloak: Keycloak): UserProfile | null {
  if (!keycloak.tokenParsed) return null;

  const token = keycloak.tokenParsed;
  // Fallback chain: given_name → name (split) → preferred_username → email
  const fullName = (token.name as string) || "";
  const firstName =
    (token.given_name as string) ||
    fullName.split(" ")[0] ||
    (token.preferred_username as string) ||
    (token.email as string)?.split("@")[0] ||
    "";
  const lastName =
    (token.family_name as string) ||
    fullName.split(" ").slice(1).join(" ") ||
    "";

  return {
    id: token.sub || "",
    email: (token.email as string) || "",
    firstName,
    lastName,
    roles: extractRoles(keycloak),
  };
}

export function hasRole(keycloak: Keycloak, role: UserRole): boolean {
  return extractRoles(keycloak).includes(role);
}
