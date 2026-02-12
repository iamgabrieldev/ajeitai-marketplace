package auth

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
)

// KeycloakClaims compatível com o access token do Keycloak.
// realm_access no Keycloak é um objeto { "roles": ["cliente", ...] }, não um array.
type KeycloakClaims struct {
	Sub               string `json:"sub"`
	RealmAccess       struct {
		Roles []string `json:"roles"`
	} `json:"realm_access"`
	PreferredUsername string `json:"preferred_username"`
	jwt.RegisteredClaims
}

func GinMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		auth := c.GetHeader("Authorization")
		if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"erro": "token ausente"})
			return
		}
		tokenStr := strings.TrimPrefix(auth, "Bearer ")
		token, _, err := jwt.NewParser().ParseUnverified(tokenStr, &KeycloakClaims{})
		if err != nil {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"erro": "token inválido"})
			return
		}
		claims, ok := token.Claims.(*KeycloakClaims)
		if !ok || claims.Sub == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"erro": "token inválido"})
			return
		}
		ctx := WithUserID(c.Request.Context(), claims.Sub)
		c.Request = c.Request.WithContext(ctx)
		c.Next()
	}
}

type contextKey string

const userIDKey contextKey = "userID"

func WithUserID(ctx context.Context, userID string) context.Context {
	return context.WithValue(ctx, userIDKey, userID)
}

func UserID(ctx context.Context) (string, bool) {
	v := ctx.Value(userIDKey)
	if v == nil {
		return "", false
	}
	s, ok := v.(string)
	return s, ok
}

func RespondJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func RespondError(w http.ResponseWriter, status int, msg string) {
	RespondJSON(w, status, map[string]string{"erro": msg})
}
