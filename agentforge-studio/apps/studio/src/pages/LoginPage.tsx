import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
  Input,
  Label,
  Typography,
  Alert,
  AlertDescription,
  Spinner,
} from "@agentforge/ui";
import { useAuthStore } from "../stores/auth-store.js";
import { t } from "../i18n/i18n.js";

/**
 * LoginPage - authentication entry point.
 *
 * ┌──────────────────────────────────────┐
 * │          AgentForge Studio           │
 * │                                      │
 * │  ┌──────────────────────────────┐    │
 * │  │  Sign In                     │    │
 * │  │                              │    │
 * │  │  Username  [____________]    │    │
 * │  │  Password  [____________]    │    │
 * │  │                              │    │
 * │  │  [      Sign In        ]     │    │
 * │  └──────────────────────────────┘    │
 * └──────────────────────────────────────┘
 */
export function LoginPage() {
  const navigate = useNavigate();
  const { login, loading, error, clearError } = useAuthStore();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();

    if (!username.trim() || !password.trim()) {
      return;
    }

    const success = await login(username.trim(), password);
    if (success) {
      navigate("/dashboard", { replace: true });
    }
  }

  return (
    <Box className="min-h-screen flex items-center justify-center bg-background p-4">
      <Box className="w-full max-w-sm space-y-6">
        {/* Branding */}
        <Box className="text-center space-y-2">
          <Typography variant="h2" className="font-bold">
            AgentForge Studio
          </Typography>
          <Typography color="muted">
            {t("login.subtitle")}
          </Typography>
        </Box>

        {/* Login Card */}
        <Card>
          <CardHeader>
            <CardTitle>{t("login.title")}</CardTitle>
            <CardDescription>
              {t("login.description")}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Error Alert */}
              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {/* Username */}
              <Box className="space-y-2">
                <Label htmlFor="username">
                  {t("login.username")}
                </Label>
                <Input
                  id="username"
                  type="text"
                  placeholder={t("login.usernamePlaceholder")}
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    clearError();
                  }}
                  autoComplete="username"
                  autoFocus
                  disabled={loading}
                />
              </Box>

              {/* Password */}
              <Box className="space-y-2">
                <Label htmlFor="password">
                  {t("login.password")}
                </Label>
                <Input
                  id="password"
                  type="password"
                  placeholder={t("login.passwordPlaceholder")}
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    clearError();
                  }}
                  autoComplete="current-password"
                  disabled={loading}
                />
              </Box>

              {/* Submit */}
              <Button
                type="submit"
                className="w-full"
                disabled={loading || !username.trim() || !password.trim()}
              >
                {loading ? (
                  <Box className="flex items-center gap-2">
                    <Spinner size="sm" />
                    {t("login.signingIn")}
                  </Box>
                ) : (
                  t("login.signIn")
                )}
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* Footer */}
        <Typography
          variant="caption"
          color="muted"
          className="text-center block"
        >
          {t("login.footer")}
        </Typography>
      </Box>
    </Box>
  );
}
