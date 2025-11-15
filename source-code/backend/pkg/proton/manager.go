package proton

import (
	"archive/tar"
	"compress/gzip"
	"epic-session/pkg/utils"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

var protonDir = strings.Replace("/home/$USER/.steam/root/compatibilitytools.d", "$USER", os.Getenv("USER"), 1)

func ListProtons() ([]utils.Proton, error) {
	files, err := os.ReadDir(protonDir)
	if err != nil {
		return nil, err
	}
	var protons []utils.Proton
	for _, f := range files {
		if f.IsDir() {
			protons = append(protons, utils.Proton{Name: f.Name(), Path: filepath.Join(protonDir, f.Name())})
		}
	}
	sort.Slice(protons, func(i, j int) bool { return protons[i].Name < protons[j].Name })
	return protons, nil
}

func InstallProton(version string) error {
	if version == "" {
		version = getLatestVersion() // Mock, w realu użyj API GitHub
	}
	url := fmt.Sprintf("https://github.com/GloriousEggroll/proton-ge-custom/releases/download/%s/%s.tar.gz", version, version)

	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	os.MkdirAll(protonDir, 0755)
	tarGzPath := filepath.Join(protonDir, version+".tar.gz")
	out, err := os.Create(tarGzPath)
	if err != nil {
		return err
	}
	io.Copy(out, resp.Body)
	out.Close()

	// Rozpakuj
	f, err := os.Open(tarGzPath)
	if err != nil {
		return err
	}
	gz, err := gzip.NewReader(f)
	if err != nil {
		return err
	}
	tr := tar.NewReader(gz)
	for {
		hdr, err := tr.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		target := filepath.Join(protonDir, hdr.Name)
		switch hdr.Typeflag {
		case tar.TypeDir:
			os.MkdirAll(target, 0755)
		case tar.TypeReg:
			file, err := os.OpenFile(target, os.O_CREATE|os.O_RDWR, os.FileMode(hdr.Mode))
			if err != nil {
				return err
			}
			io.Copy(file, tr)
			file.Close()
		}
	}
	os.Remove(tarGzPath)
	return nil
}

func getLatestVersion() string {
	// W realu: pobierz z GitHub API, ale bez net, return default
	return "GE-Proton8-25"
}
