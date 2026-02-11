"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type Keycloak from "keycloak-js";
import {
  getKeycloak,
  initKeycloak,
  getUserProfile,
  type UserProfile,
  type UserRole,
} from "@/lib/keycloak";

interface AuthContextType {
  initialized: boolean;
  authenticated: boolean;
  user: UserProfile | null;
  keycloak: Keycloak | null;
  token: string | undefined;
  hasRole: (role: UserRole) => boolean;
  login: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  initialized: false,
  authenticated: false,
  user: null,
  keycloak: null,
  token: undefined,
  hasRole: () => false,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState<UserProfile | null>(null);
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);

  useEffect(() => {
    const kc = getKeycloak();

    initKeycloak(kc)
      .then((auth: boolean) => {
        setAuthenticated(auth);
        setKeycloak(kc);
        if (auth) {
          setUser(getUserProfile(kc));
        }
        setInitialized(true);
      })
      .catch((err: unknown) => {
        console.error("Keycloak init error:", err);
        setInitialized(true);
      });

    // Token refresh
    kc.onTokenExpired = () => {
      kc.updateToken(30)
        .then((refreshed: boolean) => {
          if (refreshed) {
            setUser(getUserProfile(kc));
          }
        })
        .catch(() => {
          console.error("Failed to refresh token");
          kc.login();
        });
    };
  }, []);

  const hasRole = useCallback(
    (role: UserRole) => {
      if (!keycloak?.tokenParsed?.realm_access?.roles) return false;
      return keycloak.tokenParsed.realm_access.roles.includes(role);
    },
    [keycloak]
  );

  const login = useCallback(() => {
    keycloak?.login();
  }, [keycloak]);

  const logout = useCallback(() => {
    keycloak?.logout({ redirectUri: window.location.origin });
  }, [keycloak]);

  return (
    <AuthContext.Provider
      value={{
        initialized,
        authenticated,
        user,
        keycloak,
        token: keycloak?.token,
        hasRole,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
