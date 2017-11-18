package main

import (
	"hehe/maven"
	"strings"
	"os"
)

func main() {
	basePath := `/tmp/hexh/sdf`
	Artifacts := *maven.GetArtifacts(basePath)
	poms := make([]*maven.Artifact, 0)
	for _, item := range Artifacts {
		if item.ArtifactType == maven.POM {
			poms = append(poms, item)
		}
	}
	for _, item := range Artifacts {
		item.Untar(findParentPath("/tmp/hexh/target", item, &poms))
	}
}

func findParentPath(defaultPath string, jar *maven.Artifact, Artifacts *[]*maven.Artifact) string {
	var parent *maven.Artifact
	for _, a := range *Artifacts {
		if jar.PathName == a.PathName {
			continue
		}
		if strings.HasPrefix(jar.PathName, a.PathName) {
			if parent == nil || strings.HasPrefix(a.PathName, parent.PathName) {
				parent = a
			}
		}
	}
	if parent != nil {
		return findParentPath(defaultPath, parent, Artifacts) + string(os.PathSeparator) + parent.PathName
	}
	return defaultPath
}
