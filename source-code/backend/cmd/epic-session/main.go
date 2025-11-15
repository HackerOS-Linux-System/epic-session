package main

import (
	"epic-session/pkg/api"
	"epic-session/pkg/config"
	"epic-session/pkg/utils"
	"os"
)

func main() {
	// Ładuj konfigurację
	cfg, err := config.LoadConfig("/usr/lib/epic-session/config.yaml")
	if err != nil {
		utils.Logger.Error("Błąd ładowania config:", err)
		os.Exit(1)
	}

	// Inicjalizuj logger
	utils.InitLogger(cfg.LogLevel)

	// Ustaw venv i legendary
	api.SetupVenv(cfg.VenvPath)

	// Uruchom router
	r := api.SetupRouter(cfg)
	r.Run(cfg.APIAddress)
}
