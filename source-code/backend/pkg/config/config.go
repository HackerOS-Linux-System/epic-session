package config

import (
	"os"

	"gopkg.in/yaml.v2"
)

type Config struct {
	VenvPath   string `yaml:"venv_path"`
	APIAddress string `yaml:"api_address"`
	LogLevel   string `yaml:"log_level"`
}

func LoadConfig(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		// Default config jeśli brak pliku
		return &Config{
			VenvPath:   "/usr/lib/epic-session/venv",
			APIAddress: ":8080",
			LogLevel:   "info",
		}, nil
	}
	var cfg Config
	err = yaml.Unmarshal(data, &cfg)
	return &cfg, err
}
