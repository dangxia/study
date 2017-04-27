package maven

import (
	"io/ioutil"
	"os"
	"fmt"
	"strings"
	"encoding/xml"
	"path/filepath"
	"os/exec"
)

type Artifact struct {
	Path         string
	PomPath      string
	ArtifactType ArtifactType
	CxtPath      string
	PathName     string
}

type Project struct {
	XMLName   xml.Name `xml:"project"`
	Packaging string `xml:"packaging"`
}

type ArtifactType int

const (
	UNKNOWN ArtifactType = iota
	POM
	JAR
	WAR
)

func (a *Artifact) Untar(parentPath string) {
	target := parentPath + string(os.PathSeparator) + a.PathName
	os.MkdirAll(target, 0755)
	if a.ArtifactType == POM {
		bytes, _ := ioutil.ReadFile(a.PomPath)
		ioutil.WriteFile(target + string(os.PathSeparator) + "pom.xml", bytes, 0644)
	} else if a.ArtifactType == JAR {
		a.unJAR(target)
	} else if a.ArtifactType == WAR {
		a.unWar(target)
	}
}

func (a *Artifact) unJAR(target string) {
	srcPath := target + string(os.PathSeparator) + "src" + string(os.PathSeparator) + "main" + string(os.PathSeparator) + "java"
	os.MkdirAll(srcPath, 0755)
	Decompile(a.CxtPath, srcPath)
	UnzipAndRemove(srcPath + string(os.PathSeparator) + filepath.Base(a.CxtPath))
	copyTo(a.PomPath, target + string(os.PathSeparator) + "pom.xml")
	os.RemoveAll(srcPath + string(os.PathSeparator) + "META-INF")
}

func (a *Artifact) unWar(target string) {

	resourcesPath := target + string(os.PathSeparator) + "src" + string(os.PathSeparator) + "main" + string(os.PathSeparator) + "resources"
	webappPath := target + string(os.PathSeparator) + "src" + string(os.PathSeparator) + "main" + string(os.PathSeparator) + "webapp"
	os.MkdirAll(resourcesPath, 0755)
	os.MkdirAll(webappPath, 0755)

	Unzip(a.CxtPath, webappPath)
	os.RemoveAll(webappPath + string(os.PathSeparator) + "META-INF")
	os.RemoveAll(webappPath + string(os.PathSeparator) + "WEB-INF" + string(os.PathSeparator) + "lib")
	copyTo(a.PomPath, target + string(os.PathSeparator) + "pom.xml")

	os.Rename(webappPath + string(os.PathSeparator) + "WEB-INF" + string(os.PathSeparator) + "classes", resourcesPath)
}

func copyTo(source string, target string) {
	fi, err := os.Stat(target)
	if err == nil && fi.IsDir() {
		target = target + string(os.PathSeparator) + filepath.Base(source)
	}
	bytes, _ := ioutil.ReadFile(source)
	ioutil.WriteFile(target, bytes, 0644)
}

func UnzipAndRemove(cxtPath string) {
	cmd := exec.Command("unzip", cxtPath, "-d", filepath.Dir(cxtPath))
	cmd.Stdin = os.Stdin
	cmd.Stderr = os.Stderr
	cmd.Stdout = os.Stdout
	cmd.Run()

	os.Remove(cxtPath)
}

func Unzip(cxtPath string, targetPath string) {
	cmd := exec.Command("unzip", cxtPath, "-d", targetPath)
	cmd.Stdin = os.Stdin
	cmd.Stderr = os.Stderr
	cmd.Stdout = os.Stdout
	cmd.Run()
}

func Decompile(cxtPath string, targetPath string) {
	cmd := exec.Command("java", "-jar", "/home/hexh/develop/gits/github/fernflower/fernflower.jar", "-dgs=1", cxtPath, targetPath)
	cmd.Stdin = os.Stdin
	cmd.Stderr = os.Stderr
	cmd.Stdout = os.Stdout
	cmd.Run()
}

func GetArtifacts(basePath string) *[]*Artifact {
	artifacts, _ := ioutil.ReadDir(basePath)
	result := make([]*Artifact, 0);
	for _, artifact := range artifacts {
		verPath := basePath + string(os.PathSeparator) + artifact.Name() + string(os.PathSeparator) + "1.0"
		if _, err := os.Stat(verPath); err != nil {
			fmt.Println(verPath, "not found")
			continue
		}
		result = append(result, createArtifact(verPath))
	}
	return &result
}

func createArtifact(verPath string) *Artifact {
	pomPath := getPomPath(verPath)
	artifactType := getArtifactType(pomPath)
	cxtPath := getCxtPath(pomPath, artifactType)
	pathName := filepath.Base(filepath.Dir(verPath))
	return &Artifact{Path:verPath, PomPath:pomPath, ArtifactType:artifactType, CxtPath:cxtPath, PathName:pathName}
}
func getCxtPath(pomPath string, atype ArtifactType) string {
	if atype == UNKNOWN || atype == POM {
		return ""
	}
	baseName := filepath.Base(pomPath)
	if (atype == JAR) {
		baseName = strings.Replace(baseName, ".pom", ".jar", len(baseName));
	} else {
		baseName = strings.Replace(baseName, ".pom", ".war", len(baseName));
	}
	return filepath.Dir(pomPath) + string(os.PathSeparator) + baseName
}

func getArtifactType(pomPath string) ArtifactType {
	content, _ := ioutil.ReadFile(pomPath)
	var project Project
	_ = xml.Unmarshal(content, &project)
	switch project.Packaging {
	case `pom`:return POM
	case `war`:return WAR
	case `jar`:
	case ``:
		return JAR
	default:
		return UNKNOWN
	}
	return UNKNOWN
}

func getPomPath(verPath string) string {
	files, _ := ioutil.ReadDir(verPath)
	for _, f := range files {
		if strings.HasSuffix(f.Name(), ".pom") {
			return verPath + string(os.PathSeparator) + f.Name()
		}
	}
	return ""
}

